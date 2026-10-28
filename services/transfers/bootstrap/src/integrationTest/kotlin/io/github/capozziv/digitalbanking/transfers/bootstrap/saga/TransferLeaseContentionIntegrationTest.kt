package io.github.capozziv.digitalbanking.transfers.bootstrap.saga

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import io.github.capozziv.digitalbanking.transfers.application.usecase.ResumeDueTransfersUseCase
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs.Companion.entryCreated
import io.github.capozziv.digitalbanking.transfers.bootstrap.web.ApiClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Requests and schedulers race for the same transfers: the scheduler of the context and extra threads that run
 * the scheduler tick in a loop, as other instances would. The lease lets one run at a time, so every call that
 * reached `accounts` is a recorded attempt in `transfer_step`: none is lost, none is made twice.
 */
@TestPropertySource(
    properties = [
        "transfers.saga.scheduler-enabled=true",
        "transfers.saga.poll-interval=10ms",
        "transfers.saga.busy-attempts=1",
        "transfers.saga.retry-initial=1ms",
        "transfers.saga.retry-max=5ms",
    ],
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TransferLeaseContentionIntegrationTest : TransfersIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var resumeDue: ResumeDueTransfersUseCase

    private val api by lazy { ApiClient(port) }
    private val stubs = AccountsStubs(accountsServer)

    @BeforeEach
    fun reset() {
        accountsServer.resetAll()
        ownerJdbc.deleteTransfers()
    }

    @Test
    fun `every call to accounts is one attempt recorded in transfer_step, with requests and schedulers racing`() {
        val pairs = List(TRANSFERS) { UUID.randomUUID() to UUID.randomUUID() }
        pairs.forEach { (source, destination) ->
            stubs.accountExists(source)
            stubs.accountExists(destination)
            stubs.sequence(
                { post(urlPathMatching("/accounts/$destination/credits")) },
                aResponse().withStatus(503),
                aResponse().withStatus(503),
                entryCreated(),
            )
        }
        // Slow enough for the racing ticks to find the transfers while a run holds them.
        accountsServer.stubFor(post(urlPathMatching("/accounts/[^/]+/debits")).willReturn(entryCreated().withFixedDelay(100)))

        val racing = AtomicBoolean(true)
        val racers = Executors.newFixedThreadPool(RACERS)
        val racerResults = List(RACERS) { racers.submit { while (racing.get()) resumeDue.execute() } }
        val ids =
            try {
                val requests = Executors.newVirtualThreadPerTaskExecutor()
                val responses =
                    pairs
                        .map { (source, destination) -> requests.submit<UUID> { requestTransfer(source, destination) } }
                        .map { it.get(60, TimeUnit.SECONDS) }
                requests.close()
                responses.forEach { ownerJdbc.awaitStatus(it, "COMPLETED") }
                responses
            } finally {
                racing.set(false)
                racers.shutdown()
                assertThat(racers.awaitTermination(30, TimeUnit.SECONDS)).isTrue()
                racerResults.forEach { it.get() }
            }

        ids.zip(pairs).forEach { (id, accounts) ->
            val (source, destination) = accounts
            val steps = ownerJdbc.recordedSteps(id)
            assertThat(stubs.requestsFor(source).filter { it.url.endsWith("/debits") })
                .describedAs("debits of transfer $id")
                .hasSize(steps.count { it == "DEBIT" })
            assertThat(stubs.requestsFor(destination).filter { it.url.endsWith("/credits") })
                .describedAs("credits of transfer $id")
                .hasSize(steps.count { it == "CREDIT" })
            assertThat(steps).containsExactly("DEBIT", "CREDIT", "CREDIT", "CREDIT")
            assertThat(ownerJdbc.outboxCorrelationIds(id).keys).containsExactly("TransferCompleted")
        }
    }

    private fun requestTransfer(
        source: UUID,
        destination: UUID,
    ): UUID {
        val response =
            api.post(
                "/transfers",
                """{"sourceAccountId":"$source","destinationAccountId":"$destination","amount":100}""",
                mapOf("Idempotency-Key" to UUID.randomUUID().toString()),
            )
        assertThat(response.status).describedAs(response.rawBody).isIn(201, 202)
        return UUID.fromString(response.body!!.get("id").asString())
    }

    private companion object {
        const val TRANSFERS = 20
        const val RACERS = 4
    }
}
