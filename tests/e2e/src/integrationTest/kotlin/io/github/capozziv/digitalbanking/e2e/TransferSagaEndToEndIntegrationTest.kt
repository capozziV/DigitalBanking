package io.github.capozziv.digitalbanking.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/**
 * Done criteria 3 to 7 of stage 2, with `accounts` and `transfers` running as real processes (`java -jar` of the boot
 * jars), PostgreSQL and LocalStack in Testcontainers, and [AccountsProxy] between `transfers` and `accounts`.
 *
 * The crash scenarios are made deterministic by the proxy: it holds the saga's call to `accounts` at the exact step
 * under test, the test stops or kills a process while the call is held, and only then releases it. Every wait is a
 * condition polled with a time limit, never a blind sleep.
 *
 * The methods run in order (`@Order`) and the last one depends on the others: it checks the events, and the correlation
 * ID, of the transfers they finished. If it fails on its own precondition (a missing transfer or event type), the
 * failure is in one of the earlier tests, so look at those first.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TransferSagaEndToEndIntegrationTest {
    private val environment = EndToEndEnvironment()

    @RegisterExtension
    val processLogs = ProcessLogsOnFailureExtension { environment.processes() }

    private val accounts get() = environment.accountsApi
    private val transfers get() = environment.transfersApi

    /** Transfer id → event type its terminal status must publish, for the events test. */
    private val expectedEvents = ConcurrentHashMap<String, String>()

    /** Transfer id → `X-Correlation-Id` sent with its request, for the events test. */
    private val expectedCorrelationIds = ConcurrentHashMap<String, String>()

    @BeforeAll
    fun start() = environment.start()

    @AfterAll
    fun stop() = environment.close()

    @BeforeEach
    fun servicesRunning() {
        environment.proxy.releaseAll()
        environment.accounts.ensureRunning()
        environment.transfers.ensureRunning()
    }

    @Test
    @Order(1)
    fun `50 parallel transfers of 200 from a FREE account with 5000 end 25 COMPLETED and 25 FAILED, money conserved`() {
        val source = accounts.openAccount()
        assertThat(accounts.account(source).get("type").asString()).isEqualTo("FREE")
        accounts.credit(source, 5_000)
        val destinations = List(PARALLEL_TRANSFERS) { accounts.openAccount() }
        val everyAccount = listOf(source) + destinations
        val totalBefore = everyAccount.sumOf { accounts.balance(it) }

        val keys = List(PARALLEL_TRANSFERS) { UUID.randomUUID().toString() }
        val transferIds =
            inParallel(PARALLEL_TRANSFERS) { index ->
                val response = transfers.requestTransfer(source, destinations[index], 200, keys[index])
                assertThat(response.status).describedAs(response.body).isIn(201, 202, 422)
                response.transferId()
            }
        transferIds.forEachIndexed { index, id -> expectedCorrelationIds[id] = TransfersApiClient.correlationIdFor(keys[index]) }
        val statuses = transferIds.associateWith { transfers.awaitTerminal(it, SAGA_FINISH_TIMEOUT) }

        assertThat(statuses.values.count { it == "COMPLETED" }).isEqualTo(25)
        assertThat(statuses.values.count { it == "FAILED" }).isEqualTo(25)
        assertThat(accounts.balance(source)).isZero()
        assertThat(everyAccount.sumOf { accounts.balance(it) }).isEqualTo(totalBefore).isEqualTo(5_000)
        statuses.forEach { (id, status) -> expectedEvents[id] = if (status == "COMPLETED") "TransferCompleted" else "TransferFailed" }
    }

    @Test
    @Order(2)
    fun `20 parallel requests with the same Idempotency-Key make one transfer, one debit and one credit`() {
        val (source, destination) = fundedPair(1_000)
        val key = UUID.randomUUID().toString()

        val responses = inParallel(REPEATED_REQUESTS) { transfers.requestTransfer(source, destination, 200, key) }

        responses.forEach { assertThat(it.status).describedAs(it.body).isIn(200, 201, 202) }
        val id = responses.map { it.transferId() }.distinct().single()
        assertThat(transfers.awaitTerminal(id, SAGA_FINISH_TIMEOUT)).isEqualTo("COMPLETED")
        assertThat(environment.transfersDatabase.transferCount(key)).isEqualTo(1)
        assertThat(accounts.entryStatus(source, "transfer-$id-debit")).isEqualTo(200)
        assertThat(accounts.entryStatus(destination, "transfer-$id-credit")).isEqualTo(200)
        assertThat(accounts.entryStatus(source, "transfer-$id-compensation")).isEqualTo(404)
        assertThat(accounts.balance(source)).isEqualTo(800)
        assertThat(accounts.balance(destination)).isEqualTo(200)
        expectedEvents[id] = "TransferCompleted"
        expectedCorrelationIds[id] = TransfersApiClient.correlationIdFor(key)
    }

    @Test
    @Order(3)
    fun `accounts stopped after the debit and back before the deadline - the transfer ends COMPLETED`() {
        val (source, destination) = fundedPair(1_000)

        val id = stopAccountsAfterDebit(source, destination)
        val deadline = environment.transfersDatabase.deadline(id)
        environment.accounts.start()

        assertThat(Instant.now())
            .describedAs("accounts came back before the deadline of the transfer; otherwise this test proves nothing (slow environment)")
            .isBefore(deadline)
        assertThat(transfers.awaitTerminal(id, SAGA_FINISH_TIMEOUT)).isEqualTo("COMPLETED")
        assertThat(accounts.entryStatus(destination, "transfer-$id-credit")).isEqualTo(200)
        assertThat(accounts.entryStatus(source, "transfer-$id-compensation")).isEqualTo(404)
        assertThat(accounts.balance(source)).isEqualTo(700)
        assertThat(accounts.balance(destination)).isEqualTo(300)
        expectedEvents[id] = "TransferCompleted"
    }

    @Test
    @Order(4)
    fun `accounts stopped after the debit and back after the deadline - the transfer ends COMPENSATED, destination never credited`() {
        val (source, destination) = fundedPair(1_000)

        val id = stopAccountsAfterDebit(source, destination)
        // Past the deadline the saga only asks whether the credit happened; it keeps asking while accounts is down.
        environment.transfersDatabase.awaitStepOutcome(id, "RESOLVE_CREDIT", "TRANSIENT_FAILURE", SAGA_FINISH_TIMEOUT)
        assertThat(transfers.status(id)).isEqualTo("PENDING")
        environment.accounts.start()

        assertThat(transfers.awaitTerminal(id, SAGA_FINISH_TIMEOUT)).isEqualTo("COMPENSATED")
        assertThat(accounts.entryStatus(destination, "transfer-$id-credit")).isEqualTo(404)
        assertThat(accounts.entryStatus(source, "transfer-$id-compensation")).isEqualTo(200)
        assertThat(accounts.balance(source)).isEqualTo(1_000)
        assertThat(accounts.balance(destination)).isZero()
        expectedEvents[id] = "TransferCompensated"
    }

    @Test
    @Order(5)
    fun `transfers killed in the credit step with a valid lease - the restarted process resumes only after the lease and credits once`() {
        val (source, destination) = fundedPair(1_000)
        val hold = environment.proxy.holdNext("POST", "/accounts/$destination/credits")
        val key = UUID.randomUUID().toString()
        val request = CompletableFuture.supplyAsync { runCatching { transfers.requestTransfer(source, destination, 300, key) } }
        val id = hold.awaitHeld(CALL_HELD_TIMEOUT).transferId()
        expectedCorrelationIds[id] = TransfersApiClient.correlationIdFor(key)
        val lease = environment.transfersDatabase.lease(id)
        val deadline = environment.transfersDatabase.deadline(id)
        assertThat(lease.status).isEqualTo("PENDING")
        assertThat(lease.step).isEqualTo("CREDIT")
        assertThat(lease.lockedUntil).isAfter(Instant.now())

        environment.transfers.kill()
        val killedAt = Instant.now()
        assertThat(request.get().isFailure).describedAs("the request dies with the process").isTrue()
        // The credit was already on its way: it reaches accounts after the process that sent it is gone.
        hold.release()
        assertThat(hold.awaitForwarded(CALL_HELD_TIMEOUT)).isEqualTo(201)
        environment.transfers.start()
        val restartedAt = Instant.now()
        assertThat(restartedAt)
            .describedAs("transfers restarted after the lease ended; the lease check would prove nothing (slow environment)")
            .isBefore(lease.lockedUntil)

        assertThat(transfers.awaitTerminal(id, SAGA_FINISH_TIMEOUT)).isEqualTo("COMPLETED")
        val stepsAfterKill = environment.transfersDatabase.recordedSteps(id).filter { it.second.isAfter(killedAt) }
        assertThat(stepsAfterKill.map { it.second }).isNotEmpty.allSatisfy { assertThat(it).isAfterOrEqualTo(lease.lockedUntil) }
        // The resume happened before the deadline, so the credit was repeated, not resolved by a lookup.
        assertThat(stepsAfterKill.first().second).describedAs("first step after the restart").isBefore(deadline)
        assertThat(
            stepsAfterKill.map { it.first },
        ).describedAs("steps after the restart").doesNotContain("RESOLVE_CREDIT").contains("CREDIT")
        assertThat(accounts.entryStatus(destination, "transfer-$id-credit")).isEqualTo(200)
        assertThat(accounts.entryStatus(source, "transfer-$id-compensation")).isEqualTo(404)
        assertThat(accounts.balance(source)).isEqualTo(700)
        assertThat(accounts.balance(destination)).isEqualTo(300)
        expectedEvents[id] = "TransferCompleted"
    }

    @Test
    @Order(6)
    fun `transfers killed in the debit step before the call reaches accounts - the restarted process finishes the transfer`() {
        val (source, destination) = fundedPair(1_000)
        val hold = environment.proxy.holdNext("POST", "/accounts/$source/debits")
        val key = UUID.randomUUID().toString()
        val request = CompletableFuture.supplyAsync { runCatching { transfers.requestTransfer(source, destination, 300, key) } }
        val id = hold.awaitHeld(CALL_HELD_TIMEOUT).transferId()
        expectedCorrelationIds[id] = TransfersApiClient.correlationIdFor(key)
        assertThat(environment.transfersDatabase.lease(id).step).isEqualTo("DEBIT")
        val deadline = environment.transfersDatabase.deadline(id)

        environment.transfers.kill()
        val killedAt = Instant.now()
        assertThat(request.get().isFailure).describedAs("the request dies with the process").isTrue()
        hold.drop()
        assertThat(accounts.entryStatus(source, "transfer-$id-debit")).isEqualTo(404)
        environment.transfers.start()

        assertThat(transfers.awaitTerminal(id, SAGA_FINISH_TIMEOUT)).isEqualTo("COMPLETED")
        val stepsAfterKill = environment.transfersDatabase.recordedSteps(id).filter { it.second.isAfter(killedAt) }
        assertThat(stepsAfterKill.first().second).describedAs("first step after the restart").isBefore(deadline)
        assertThat(
            stepsAfterKill.map { it.first },
        ).describedAs("steps after the restart").contains("DEBIT").noneMatch { it.startsWith("RESOLVE") }
        assertThat(accounts.balance(source)).isEqualTo(700)
        assertThat(accounts.balance(destination)).isEqualTo(300)
        expectedEvents[id] = "TransferCompleted"
    }

    @Test
    @Order(7)
    fun `the inspection queue receives one event of the right type for each finished transfer, of all three types`() {
        assertThat(expectedEvents.values.toSet())
            .describedAs("transfers finished by the previous tests")
            .containsExactlyInAnyOrder("TransferCompleted", "TransferFailed", "TransferCompensated")

        val received = environment.transferEvents.awaitEvents(expectedEvents.keys, EVENTS_TIMEOUT)

        expectedEvents.forEach { (id, eventType) ->
            assertThat(received[id]?.map { it.eventType }).describedAs("events of transfer $id").containsExactly(eventType)
        }
        assertThat(expectedCorrelationIds).describedAs("transfers with a known correlation ID").isNotEmpty
        expectedCorrelationIds.forEach { (id, correlationId) ->
            val events = checkNotNull(received[id])
            assertThat(events).describedAs("events of transfer $id").allSatisfy {
                assertThat(it.attributeCorrelationId).describedAs("correlationId attribute").isEqualTo(correlationId)
                assertThat(it.envelopeCorrelationId).describedAs("correlationId in the envelope").isEqualTo(correlationId)
            }
        }
    }

    /**
     * Holds the credit of a new transfer at the proxy (so the debit is already recorded), stops `accounts` while the
     * credit is held and then lets the credit go to the stopped service. Returns the id of the transfer, now PENDING.
     */
    private fun stopAccountsAfterDebit(
        source: UUID,
        destination: UUID,
    ): String {
        val hold = environment.proxy.holdNext("POST", "/accounts/$destination/credits")
        val key = UUID.randomUUID().toString()
        val request = CompletableFuture.supplyAsync { transfers.requestTransfer(source, destination, 300, key) }
        val id = hold.awaitHeld(CALL_HELD_TIMEOUT).transferId()
        expectedCorrelationIds[id] = TransfersApiClient.correlationIdFor(key)
        assertThat(accounts.entryStatus(source, "transfer-$id-debit")).isEqualTo(200)

        environment.accounts.stop()
        assertThat(environment.accounts.isRunning).describedAs("accounts is down").isFalse()
        hold.release()
        assertThat(
            hold.awaitForwarded(CALL_HELD_TIMEOUT),
        ).describedAs("the proxy answered the released call as a gateway with accounts down").isEqualTo(502)

        val response = request.get()
        assertThat(response.status).describedAs(response.body).isEqualTo(202)
        assertThat(response.json().get("status").asString()).isEqualTo("PENDING")
        environment.transfersDatabase.awaitStepOutcome(id, "CREDIT", "TRANSIENT_FAILURE", CALL_HELD_TIMEOUT)
        return id
    }

    private fun fundedPair(amount: Long): Pair<UUID, UUID> {
        val source = accounts.openAccount()
        accounts.credit(source, amount)
        return source to accounts.openAccount()
    }

    /** Runs [task] [count] times at once, released together by a latch, and returns the results in index order. */
    private fun <T> inParallel(
        count: Int,
        task: (Int) -> T,
    ): List<T> {
        val start = CountDownLatch(1)
        Executors.newFixedThreadPool(count).use { pool ->
            val futures =
                (0 until count).map { index ->
                    pool.submit<T> {
                        start.await()
                        task(index)
                    }
                }
            start.countDown()
            return futures.map { it.get() }
        }
    }

    private companion object {
        const val PARALLEL_TRANSFERS = 50
        const val REPEATED_REQUESTS = 20
        val CALL_HELD_TIMEOUT: Duration = Duration.ofSeconds(30)
        val SAGA_FINISH_TIMEOUT: Duration = Duration.ofSeconds(90)
        val EVENTS_TIMEOUT: Duration = Duration.ofSeconds(60)
    }
}
