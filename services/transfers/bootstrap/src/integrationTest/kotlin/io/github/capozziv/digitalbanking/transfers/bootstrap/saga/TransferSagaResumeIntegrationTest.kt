package io.github.capozziv.digitalbanking.transfers.bootstrap.saga

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs.Companion.entriesPath
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs.Companion.entryCreated
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs.Companion.problem
import io.github.capozziv.digitalbanking.transfers.bootstrap.web.ApiClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * The scheduler finishes what the request left pending. One call per step in the request (`busy-attempts=1`), so a
 * single `503` leaves the transfer `PENDING`; short waits and a 3 s deadline keep the tests fast.
 */
@TestPropertySource(
    properties = [
        "transfers.saga.scheduler-enabled=true",
        "transfers.saga.poll-interval=100ms",
        "transfers.saga.busy-attempts=1",
        "transfers.saga.retry-initial=100ms",
        "transfers.saga.retry-max=300ms",
        "transfers.saga.deadline=3s",
    ],
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TransferSagaResumeIntegrationTest : TransfersIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }
    private val stubs = AccountsStubs(accountsServer)
    private val mapper = JsonMapper.builder().build()
    private val source = UUID.randomUUID()
    private val destination = UUID.randomUUID()

    @BeforeEach
    fun reset() {
        accountsServer.resetAll()
        ownerJdbc.deleteTransfers()
        stubs.accountExists(source)
        stubs.accountExists(destination)
    }

    @Test
    fun `a credit refused twice with 503 is completed by the scheduler under the original correlation id`() {
        accountsServer.stubFor(post(urlEqualTo("/accounts/$source/debits")).willReturn(entryCreated()))
        stubs.sequence(
            { post(urlEqualTo("/accounts/$destination/credits")) },
            aResponse().withStatus(503),
            aResponse().withStatus(503),
            entryCreated(),
        )

        val response = requestTransfer("corr-resume-1")

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(202)
        assertThat(response.body!!.get("status").asString()).isEqualTo("PENDING")
        val id = UUID.fromString(response.body.get("id").asString())
        ownerJdbc.awaitStatus(id, "COMPLETED")

        val credits = stubs.requestsFor(destination)
        assertThat(credits).hasSize(3)
        assertThat(credits).allSatisfy { assertThat(it.getHeader("X-Correlation-Id")).isEqualTo("corr-resume-1") }
        assertThat(ownerJdbc.recordedSteps(id)).containsExactly("DEBIT", "CREDIT", "CREDIT", "CREDIT")
        assertThat(ownerJdbc.outboxCorrelationIds(id)).containsExactlyEntriesOf(mapOf("TransferCompleted" to "corr-resume-1"))
        assertThat(
            api
                .get("/transfers/$id")
                .body!!
                .get("status")
                .asString(),
        ).isEqualTo("COMPLETED")
    }

    @Test
    fun `a credit failing until the deadline is looked up, not found, and compensated with the compensation key`() {
        accountsServer.stubFor(post(urlEqualTo("/accounts/$source/debits")).willReturn(entryCreated()))
        accountsServer.stubFor(post(urlEqualTo("/accounts/$destination/credits")).willReturn(aResponse().withStatus(503)))
        accountsServer.stubFor(get(entriesPath(destination)).willReturn(problem(404, "entry-not-found")))
        accountsServer.stubFor(post(urlEqualTo("/accounts/$source/credits")).willReturn(entryCreated()))

        val response = requestTransfer("corr-compensate-1")

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(202)
        val id = UUID.fromString(response.body!!.get("id").asString())
        ownerJdbc.awaitStatus(id, "COMPENSATED")

        val lookups = stubs.requestsFor(destination).filter { it.url.contains("/entries") }
        assertThat(lookups).isNotEmpty().allSatisfy {
            assertThat(it.queryParameter("idempotencyKey").firstValue()).isEqualTo("transfer-$id-credit")
        }
        val compensations = stubs.requestsFor(source).filter { it.url == "/accounts/$source/credits" }
        assertThat(compensations).hasSize(1)
        assertThat(compensations.single().getHeader("Idempotency-Key")).isEqualTo("transfer-$id-compensation")
        assertThat(mapper.readTree(compensations.single().bodyAsString).get("description").asString())
            .isEqualTo("Transfer $id compensation")
        assertThat(compensations.single().getHeader("X-Correlation-Id")).isEqualTo("corr-compensate-1")
        assertThat(ownerJdbc.recordedSteps(id)).endsWith("RESOLVE_CREDIT", "COMPENSATE")
        assertThat(ownerJdbc.outboxCorrelationIds(id)).containsExactlyEntriesOf(mapOf("TransferCompensated" to "corr-compensate-1"))
    }

    /**
     * What a restarted instance finds after a crash: a pending transfer whose lease, taken by the process that died,
     * has expired. The scheduler of this (new) context takes it over and finishes it.
     */
    @Test
    fun `a pending transfer whose owner died is finished once its lease expires`() {
        val id = UUID.randomUUID()
        val now = Instant.now()
        insertPendingInCredit(id, lockedUntil = now.minusSeconds(1), now = now)
        accountsServer.stubFor(post(urlEqualTo("/accounts/$destination/credits")).willReturn(entryCreated()))

        ownerJdbc.awaitStatus(id, "COMPLETED")

        val credits = stubs.requestsFor(destination)
        assertThat(credits).hasSize(1)
        assertThat(credits.single().getHeader("Idempotency-Key")).isEqualTo("transfer-$id-credit")
        assertThat(credits.single().getHeader("X-Correlation-Id")).isEqualTo("corr-dead-process")
        assertThat(ownerJdbc.outboxCorrelationIds(id)).containsExactlyEntriesOf(mapOf("TransferCompleted" to "corr-dead-process"))
    }

    @Test
    fun `a pending transfer whose lease is still valid is left alone`() {
        val id = UUID.randomUUID()
        val now = Instant.now()
        insertPendingInCredit(id, lockedUntil = now.plusSeconds(60), now = now)
        accountsServer.stubFor(post(urlEqualTo("/accounts/$destination/credits")).willReturn(entryCreated()))

        Thread.sleep(1_000)

        assertThat(ownerJdbc.statusOf(id)).isEqualTo("PENDING")
        assertThat(stubs.requestsFor(destination)).isEmpty()
    }

    private fun requestTransfer(correlationId: String) =
        api.post(
            "/transfers",
            """{"sourceAccountId":"$source","destinationAccountId":"$destination","amount":2500,"description":"rent"}""",
            mapOf("Idempotency-Key" to UUID.randomUUID().toString(), "X-Correlation-Id" to correlationId),
        )

    private fun insertPendingInCredit(
        id: UUID,
        lockedUntil: Instant,
        now: Instant,
    ) {
        ownerJdbc
            .sql(
                """
                insert into transfer (id, source_account_id, destination_account_id, amount, description, idempotency_key,
                                      status, step, deadline_at, next_attempt_at, attempts, locked_until, lock_owner,
                                      correlation_id, created_at, updated_at)
                values (:id, :source, :destination, 2500, 'rent', :key, 'PENDING', 'CREDIT', :deadline, :past, 1,
                        :lockedUntil, 'request-of-a-dead-process', 'corr-dead-process', :past, :past)
                """.trimIndent(),
            ).param("id", id)
            .param("source", source)
            .param("destination", destination)
            .param("key", "key-$id")
            .param("deadline", now.plusSeconds(120).utc())
            .param("past", now.minusSeconds(5).utc())
            .param("lockedUntil", lockedUntil.utc())
            .update()
    }

    private fun Instant.utc(): OffsetDateTime = OffsetDateTime.ofInstant(this, ZoneOffset.UTC)
}
