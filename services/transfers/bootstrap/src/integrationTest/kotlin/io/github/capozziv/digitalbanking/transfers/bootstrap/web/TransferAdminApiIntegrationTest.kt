package io.github.capozziv.digitalbanking.transfers.bootstrap.web

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs.Companion.entriesPath
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs.Companion.entryCreated
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs.Companion.problem
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.deleteTransfers
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.outboxCorrelationIds
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.statusOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * The admin's way out of a compensation that `accounts` keeps refusing (a bug): list the stuck transfers, retry the
 * compensation now, or compensate by hand with a reason and the credit the admin recorded on the source account, which
 * `accounts` must confirm. Admins only: a customer or a service token gets 403.
 */
class TransferAdminApiIntegrationTest : TransfersIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val adminId = UUID.randomUUID()
    private val admin by lazy { ApiClient(port, JwtTestSupport.adminToken(adminId)) }
    private val customer by lazy { ApiClient(port) }
    private val source = UUID.randomUUID()
    private val destination = UUID.randomUUID()

    @BeforeEach
    fun reset() {
        accountsServer.resetAll()
        ownerDataSource.deleteTransfers()
    }

    // --- who ---

    @Test
    fun `the admin routes are 401 without a token and 403 for a customer or a service`() {
        val id = givenStuckCompensation()
        val anonymous = admin.withToken(null)
        val service = admin.withToken(JwtTestSupport.serviceToken())

        for (client in listOf(customer, service)) {
            assertProblem(client.get("/admin/transfers/stuck"), 403, "forbidden")
            assertProblem(client.send(HttpMethod.POST, "/admin/transfers/$id/retry"), 403, "forbidden")
            assertProblem(client.post("/admin/transfers/$id/manual-compensation", reason("x")), 403, "forbidden")
        }
        assertProblem(anonymous.get("/admin/transfers/stuck"), 401, "unauthorized")
        assertProblem(anonymous.send(HttpMethod.POST, "/admin/transfers/$id/retry"), 401, "unauthorized")
        assertProblem(anonymous.post("/admin/transfers/$id/manual-compensation", reason("x")), 401, "unauthorized")
        assertThat(ownerJdbc.statusOf(id)).isEqualTo("PENDING")
        assertThat(resolutionCount(id)).isZero()
        assertThat(accountsServer.allServeEvents).isEmpty()
    }

    // --- list ---

    @Test
    fun `lists only pending transfers whose last compensation was refused, oldest first`() {
        val newer = givenStuckCompensation(createdAgo = 60, refusal = "account-closed")
        val older = givenStuckCompensation(createdAgo = 600, refusal = "http-409")
        val retryingAfterTransientFailure = givenTransfer("COMPENSATE", createdAgo = 900)
        stepRecord(retryingAfterTransientFailure, "COMPENSATE", "REFUSED", "account-closed", agoSeconds = 30)
        stepRecord(retryingAfterTransientFailure, "COMPENSATE", "TRANSIENT_FAILURE", null, agoSeconds = 10)
        givenTransfer("CREDIT", createdAgo = 900).also { stepRecord(it, "CREDIT", "REFUSED", "account-blocked", agoSeconds = 5) }

        val response = admin.get("/admin/transfers/stuck")

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        val items = response.body!!
        assertThat(
            (0 until items.size()).map {
                items
                    .get(it)
                    .get("transfer")
                    .get("id")
                    .asString()
            },
        ).containsExactly(older.toString(), newer.toString())
        val first = items.get(0)
        assertThat(first.get("refusal").asString()).isEqualTo("http-409")
        assertThat(first.get("transfer").get("status").asString()).isEqualTo("PENDING")
        assertThat(first.get("transfer").get("amount").asLong()).isEqualTo(2500)
        assertThat(first.get("step").asString()).isEqualTo("COMPENSATE")
        assertThat(first.has("refusedAt")).isTrue()
    }

    // --- retry now ---

    @Test
    fun `retrying a stuck compensation that accounts now accepts ends it compensated`() {
        val id = givenStuckCompensation()
        accountsServer.stubFor(post(urlEqualTo("/accounts/$source/credits")).willReturn(entryCreated()))

        val response = admin.send(HttpMethod.POST, "/admin/transfers/$id/retry")

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("status").asString()).isEqualTo("COMPENSATED")
        accountsServer.verify(
            postRequestedFor(urlEqualTo("/accounts/$source/credits"))
                .withHeader("Idempotency-Key", equalTo("transfer-$id-compensation"))
                .withHeader("X-Correlation-Id", equalTo("corr-stuck")),
        )
        assertThat(ownerJdbc.outboxCorrelationIds(id)).containsExactlyEntriesOf(mapOf("TransferCompensated" to "corr-stuck"))
    }

    @Test
    fun `retrying a compensation refused again answers it still pending`() {
        val id = givenStuckCompensation()
        accountsServer.stubFor(post(urlEqualTo("/accounts/$source/credits")).willReturn(problem(409, "account-closed")))

        val response = admin.send(HttpMethod.POST, "/admin/transfers/$id/retry")

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("status").asString()).isEqualTo("PENDING")
        assertThat(ownerJdbc.outboxCorrelationIds(id)).isEmpty()
    }

    @Test
    fun `retrying an unknown transfer is 404, a finished one 409, one in a saga run 409 busy`() {
        assertProblem(admin.send(HttpMethod.POST, "/admin/transfers/${UUID.randomUUID()}/retry"), 404, "transfer-not-found")
        val done = givenTransfer("COMPLETED")
        assertProblem(admin.send(HttpMethod.POST, "/admin/transfers/$done/retry"), 409, "transfer-not-pending")
        val leased = givenStuckCompensation()
        holdLease(leased)
        assertProblem(admin.send(HttpMethod.POST, "/admin/transfers/$leased/retry"), 409, "transfer-busy")
        assertThat(accountsServer.allServeEvents).isEmpty()
    }

    // --- compensate by hand ---

    @Test
    fun `compensating by hand ends it compensated, records the admin, the reason and the credit, and emits the event`() {
        val id = givenStuckCompensation()
        givenCreditInAccounts()

        val response = admin.post("/admin/transfers/$id/manual-compensation", reason("credited back by the back office, ticket 42"))

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        val body = response.body!!
        assertThat(body.get("transfer").get("status").asString()).isEqualTo("COMPENSATED")
        assertThat(body.get("resolvedBy").asString()).isEqualTo(adminId.toString())
        assertThat(body.get("reason").asString()).isEqualTo("credited back by the back office, ticket 42")
        assertThat(ownerJdbc.statusOf(id)).isEqualTo("COMPENSATED")
        val row =
            ownerJdbc
                .sql("select resolved_by, reason, credit_idempotency_key from transfer_manual_resolution where transfer_id = :id")
                .param("id", id)
                .query { rs, _ ->
                    Triple(rs.getObject("resolved_by", UUID::class.java), rs.getString("reason"), rs.getString("credit_idempotency_key"))
                }.single()
        assertThat(row).isEqualTo(Triple(adminId, "credited back by the back office, ticket 42", CREDIT_KEY))
        assertThat(body.get("creditIdempotencyKey").asString()).isEqualTo(CREDIT_KEY)
        assertThat(ownerJdbc.outboxCorrelationIds(id)).containsExactlyEntriesOf(mapOf("TransferCompensated" to "corr-stuck"))
        assertThat(nextAttemptAt(id)).isNull()
        accountsServer.verify(
            getRequestedFor(entriesPath(source))
                .withQueryParam("idempotencyKey", equalTo(CREDIT_KEY))
                .withHeader("X-Correlation-Id", equalTo("corr-stuck")),
        )
        assertThat(accountsServer.allServeEvents).hasSize(1)
        assertThat(ownerJdbc.sql("select count(*) from transfer_manual_resolution").query(Int::class.java).single()).isEqualTo(1)
    }

    @Test
    fun `compensating by hand twice answers the first resolution and writes nothing more`() {
        val id = givenStuckCompensation()
        givenCreditInAccounts()
        val first = admin.post("/admin/transfers/$id/manual-compensation", reason("first"))

        val second =
            admin.withToken(JwtTestSupport.adminToken(UUID.randomUUID())).post("/admin/transfers/$id/manual-compensation", reason("second"))

        assertThat(second.status).describedAs(second.rawBody).isEqualTo(200)
        assertThat(second.body).isEqualTo(first.body)
        assertThat(ownerJdbc.outboxCorrelationIds(id)).hasSize(1)
    }

    @Test
    fun `compensating by hand needs a reason, a credit key, a transfer in compensate and a free lease`() {
        val id = givenStuckCompensation()
        givenCreditInAccounts()
        val blank = admin.post("/admin/transfers/$id/manual-compensation", reason("  "))
        assertProblem(blank, 400, "validation-error")
        assertThat(blank.errorFields()).containsExactly("reason")
        assertProblem(admin.post("/admin/transfers/$id/manual-compensation", reason("x".repeat(501))), 400, "validation-error")
        val noKey = admin.post("/admin/transfers/$id/manual-compensation", """{"reason":"x"}""")
        assertProblem(noKey, 400, "validation-error")
        assertThat(noKey.errorFields()).containsExactly("creditIdempotencyKey")
        assertProblem(admin.post("/admin/transfers/$id/manual-compensation", reason("x", " ")), 400, "validation-error")
        assertProblem(admin.post("/admin/transfers/$id/manual-compensation", reason("x", "k".repeat(256))), 400, "validation-error")
        assertProblem(admin.post("/admin/transfers/$id/manual-compensation", "{}"), 400, "validation-error")
        assertThat(accountsServer.allServeEvents).isEmpty()

        val inCredit = givenTransfer("CREDIT")
        assertProblem(admin.post("/admin/transfers/$inCredit/manual-compensation", reason("x")), 409, "transfer-not-compensating")
        assertProblem(admin.post("/admin/transfers/${UUID.randomUUID()}/manual-compensation", reason("x")), 404, "transfer-not-found")

        holdLease(id)
        assertProblem(admin.post("/admin/transfers/$id/manual-compensation", reason("x")), 409, "transfer-busy")
        assertThat(ownerJdbc.statusOf(id)).isEqualTo("PENDING")
        assertThat(resolutionCount(id)).isZero()
    }

    @Test
    fun `concurrent manual compensations of the same transfer write one resolution and one event`() {
        val id = givenStuckCompensation()
        givenCreditInAccounts()
        val threads = 8
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(threads)
        val statuses =
            (1..threads).map { n ->
                pool.submit<Int> {
                    start.await()
                    admin
                        .withToken(
                            JwtTestSupport.adminToken(UUID.randomUUID()),
                        ).post("/admin/transfers/$id/manual-compensation", reason("r$n"))
                        .status
                }
            }

        start.countDown()
        val answered = statuses.map { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()

        // Every answer is 200 (the resolution, first or repeated) or 409 (transfer-busy while another holds the lease);
        // how many of each depends on the interleaving: requests that run one after another all answer 200.
        assertThat(answered).isSubsetOf(200, 409).contains(200)
        assertThat(resolutionCount(id)).isEqualTo(1)
        assertThat(ownerJdbc.outboxCorrelationIds(id)).containsExactlyEntriesOf(mapOf("TransferCompensated" to "corr-stuck"))
        assertThat(ownerJdbc.statusOf(id)).isEqualTo("COMPENSATED")
    }

    @Test
    fun `a credit accounts does not know is 422 and the transfer stays stuck`() {
        val id = givenStuckCompensation()
        accountsServer.stubFor(get(entriesPath(source)).willReturn(problem(404, "entry-not-found")))

        val response = admin.post("/admin/transfers/$id/manual-compensation", reason("trust me"))

        assertProblem(response, 422, "compensation-credit-not-found")
        assertThat(response.body!!.get("creditIdempotencyKey").asString()).isEqualTo(CREDIT_KEY)
        assertStillStuck(id)
    }

    @Test
    fun `a credit of another amount or a debit under the key is 422 and the transfer stays stuck`() {
        val id = givenStuckCompensation()

        givenCreditInAccounts(amount = 2499)
        val less = admin.post("/admin/transfers/$id/manual-compensation", reason("x"))
        assertProblem(less, 422, "compensation-credit-mismatch")
        assertThat(less.body!!.get("detail").asString()).contains("2499 cents")

        givenCreditInAccounts(kind = "DEBIT")
        assertProblem(admin.post("/admin/transfers/$id/manual-compensation", reason("x")), 422, "compensation-credit-mismatch")
        assertStillStuck(id)
    }

    @Test
    fun `accounts not answering is 503 and the transfer stays stuck`() {
        val id = givenStuckCompensation()
        accountsServer.stubFor(get(entriesPath(source)).willReturn(problem(503, "account-busy")))

        val response = admin.post("/admin/transfers/$id/manual-compensation", reason("x"))

        assertProblem(response, 503, "accounts-unavailable")
        assertStillStuck(id)
    }

    @Test
    fun `one credit compensates one transfer only`() {
        val first = givenStuckCompensation()
        val second = givenStuckCompensation()
        givenCreditInAccounts()
        assertThat(admin.post("/admin/transfers/$first/manual-compensation", reason("x")).status).isEqualTo(200)

        val again = admin.post("/admin/transfers/$second/manual-compensation", reason("same credit"))

        assertProblem(again, 409, "compensation-credit-already-used")
        assertStillStuck(second)
    }

    @Test
    fun `concurrent manual compensations of two transfers with the same credit compensate only one`() {
        val transfers = (1..4).map { givenStuckCompensation() }
        givenCreditInAccounts()
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(transfers.size)
        val statuses =
            transfers.map { id ->
                pool.submit<Int> {
                    start.await()
                    admin.post("/admin/transfers/$id/manual-compensation", reason("race")).status
                }
            }

        start.countDown()
        val answered = statuses.map { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()

        assertThat(answered.count { it == 200 }).isEqualTo(1)
        assertThat(answered.filter { it != 200 }).containsOnly(409)
        assertThat(ownerJdbc.sql("select count(*) from transfer_manual_resolution").query(Int::class.java).single()).isEqualTo(1)
        assertThat(transfers.map { ownerJdbc.statusOf(it) }.count { it == "COMPENSATED" }).isEqualTo(1)
        assertThat(transfers.sumOf { ownerJdbc.outboxCorrelationIds(it).size }).isEqualTo(1)
    }

    // --- helpers ---

    private fun reason(
        text: String,
        creditKey: String = CREDIT_KEY,
    ) = """{"reason":"$text","creditIdempotencyKey":"$creditKey"}"""

    /** `accounts` answers the read of the entry under [CREDIT_KEY] on the source account. */
    private fun givenCreditInAccounts(
        amount: Long = 2500,
        kind: String = "CREDIT",
    ) {
        accountsServer.stubFor(
            get(entriesPath(source))
                .withQueryParam("idempotencyKey", equalTo(CREDIT_KEY))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """{"id":"${UUID.randomUUID()}","accountId":"$source","sequence":7,"kind":"$kind","amount":$amount,""" +
                                """"balanceAfter":2500,"description":"back office","createdAt":"2026-11-16T10:00:00Z"}""",
                        ),
                ),
        )
    }

    private fun assertStillStuck(id: UUID) {
        assertThat(ownerJdbc.statusOf(id)).isEqualTo("PENDING")
        assertThat(resolutionCount(id)).isZero()
        assertThat(ownerJdbc.outboxCorrelationIds(id)).isEmpty()
        accountsServer.verify(0, postRequestedFor(urlEqualTo("/accounts/$source/credits")))
    }

    private fun givenStuckCompensation(
        createdAgo: Long = 300,
        refusal: String = "account-closed",
    ): UUID =
        givenTransfer("COMPENSATE", createdAgo).also {
            stepRecord(it, "CREDIT", "REFUSED", "account-closed", agoSeconds = createdAgo - 1)
            stepRecord(it, "COMPENSATE", "REFUSED", refusal, agoSeconds = 20)
        }

    private fun givenTransfer(
        step: String,
        createdAgo: Long = 300,
    ): UUID {
        val id = UUID.randomUUID()
        val created = Instant.now().minusSeconds(createdAgo).truncatedTo(ChronoUnit.MICROS)
        val status =
            when (step) {
                "COMPLETED", "FAILED", "COMPENSATED" -> step
                else -> "PENDING"
            }
        ownerJdbc
            .sql(
                """
                insert into transfer (id, source_account_id, destination_account_id, amount, description, idempotency_key,
                                      status, step, deadline_at, next_attempt_at, attempts, correlation_id, created_at,
                                      updated_at, limit_day, limit_reserved, source_customer_id)
                values (:id, :source, :destination, 2500, 'rent', :key, :status, :step, :deadline, :nextAttempt, 3,
                        'corr-stuck', :created, :created, (:created at time zone 'America/Sao_Paulo')::date, false, :customer)
                """.trimIndent(),
            ).param("id", id)
            .param("source", source)
            .param("destination", destination)
            .param("key", "key-$id")
            .param("status", status)
            .param("step", step)
            .param("deadline", created.plusSeconds(120).utc())
            // Far in the future: only the admin's retry runs the saga here (the scheduler is off anyway).
            .param("nextAttempt", if (status == "PENDING") Instant.now().plusSeconds(3600).utc() else null)
            .param("created", created.utc())
            .param("customer", CUSTOMER_ID)
            .update()
        return id
    }

    private fun stepRecord(
        id: UUID,
        step: String,
        outcome: String,
        detail: String?,
        agoSeconds: Long,
    ) {
        ownerJdbc
            .sql(
                """
                insert into transfer_step (transfer_id, step, outcome, http_status, detail, created_at)
                values (:id, :step, :outcome, :httpStatus, :detail, :at)
                """.trimIndent(),
            ).param("id", id)
            .param("step", step)
            .param("outcome", outcome)
            .param("httpStatus", if (outcome == "REFUSED") 409 else null)
            .param("detail", detail)
            .param("at", Instant.now().minusSeconds(agoSeconds).utc())
            .update()
    }

    private fun holdLease(id: UUID) {
        ownerJdbc
            .sql("update transfer set lock_owner = 'scheduler-run', locked_until = :until where id = :id")
            .param("until", Instant.now().plusSeconds(60).utc())
            .param("id", id)
            .update()
    }

    private fun resolutionCount(id: UUID): Int =
        ownerJdbc
            .sql("select count(*) from transfer_manual_resolution where transfer_id = :id")
            .param("id", id)
            .query(Int::class.java)
            .single()

    private fun nextAttemptAt(id: UUID): OffsetDateTime? =
        ownerJdbc
            .sql("select next_attempt_at from transfer where id = :id")
            .param("id", id)
            .query { rs, _ -> rs.getObject("next_attempt_at", OffsetDateTime::class.java) }
            .list()
            .single()

    private fun Instant.utc(): OffsetDateTime = OffsetDateTime.ofInstant(this, ZoneOffset.UTC)

    private companion object {
        const val CREDIT_KEY = "back-office-credit-42"
    }
}
