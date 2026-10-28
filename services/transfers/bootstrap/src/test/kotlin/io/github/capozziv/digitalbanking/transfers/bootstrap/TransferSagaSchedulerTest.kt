package io.github.capozziv.digitalbanking.transfers.bootstrap

import io.github.capozziv.digitalbanking.transfers.application.RetryBackoff
import io.github.capozziv.digitalbanking.transfers.application.port.AccountLookup
import io.github.capozziv.digitalbanking.transfers.application.port.AccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.port.CallResult
import io.github.capozziv.digitalbanking.transfers.application.port.CorrelationContext
import io.github.capozziv.digitalbanking.transfers.application.port.EventOutbox
import io.github.capozziv.digitalbanking.transfers.application.port.StepRecord
import io.github.capozziv.digitalbanking.transfers.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.application.port.TransferStepLog
import io.github.capozziv.digitalbanking.transfers.application.usecase.ResumeDueTransfersUseCase
import io.github.capozziv.digitalbanking.transfers.application.usecase.RunTransferSagaUseCase
import io.github.capozziv.digitalbanking.transfers.application.usecase.SagaSettings
import io.github.capozziv.digitalbanking.transfers.domain.AccountsCall
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import io.github.capozziv.digitalbanking.transfers.domain.TransferEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TransferSagaSchedulerTest {
    private val repository = BlockingRepository()
    private val resumeDue = resumeDueOver(repository)

    @Test
    fun `a tick looks for due transfers`() {
        TransferSagaScheduler(resumeDue, shutdownTimeout = Duration.ofSeconds(5)).runTick()

        assertThat(repository.findDueCalls.get()).isEqualTo(1)
    }

    @Test
    fun `shutdown waits for the tick in progress to finish`() {
        val scheduler = TransferSagaScheduler(resumeDue, shutdownTimeout = Duration.ofSeconds(5))
        repository.blockNextTick()
        val tick = Thread.ofPlatform().start { scheduler.runTick() }
        assertThat(repository.tickEntered.await(5, TimeUnit.SECONDS)).isTrue()

        val shutdown = Thread.ofPlatform().start { scheduler.destroy().also { repository.steps += "shutdown done" } }
        shutdown.join(300)
        assertThat(shutdown.isAlive).describedAs("shutdown returned while a tick was running").isTrue()

        repository.release.countDown()
        tick.join(5_000)
        shutdown.join(5_000)
        assertThat(repository.steps).containsExactly("tick done", "shutdown done")
    }

    @Test
    fun `no tick starts after shutdown`() {
        val scheduler = TransferSagaScheduler(resumeDue, shutdownTimeout = Duration.ofSeconds(5))

        scheduler.destroy()
        scheduler.runTick()

        assertThat(repository.findDueCalls.get()).isZero()
    }

    @Test
    fun `shutdown gives up after the timeout when a tick does not finish`() {
        val scheduler = TransferSagaScheduler(resumeDue, shutdownTimeout = Duration.ofMillis(200))
        repository.blockNextTick()
        val tick = Thread.ofPlatform().start { scheduler.runTick() }
        assertThat(repository.tickEntered.await(5, TimeUnit.SECONDS)).isTrue()

        val started = System.nanoTime()
        scheduler.destroy()
        val waited = Duration.ofNanos(System.nanoTime() - started)

        assertThat(waited).isBetween(Duration.ofMillis(150), Duration.ofSeconds(3))
        repository.release.countDown()
        tick.join(5_000)
    }

    @Test
    fun `after shutdown the batch in progress does not start the next transfer`() {
        val scheduler = TransferSagaScheduler(resumeDue, shutdownTimeout = Duration.ofSeconds(5))
        repository.dueIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        repository.blockInFindById = true
        val tick = Thread.ofPlatform().start { scheduler.runTick() }
        assertThat(repository.findByIdEntered.await(5, TimeUnit.SECONDS)).isTrue()

        val shutdown = Thread.ofPlatform().start { scheduler.destroy() }
        shutdown.join(300)
        repository.release.countDown()
        tick.join(5_000)
        shutdown.join(5_000)

        assertThat(repository.findByIdCalls.get()).isEqualTo(1)
    }

    /** Only [findDue] is reached: it returns no transfer, so the saga itself never runs. */
    private class BlockingRepository : TransferRepository {
        val findDueCalls = AtomicInteger()
        val steps: MutableList<String> = Collections.synchronizedList(mutableListOf())
        val tickEntered = CountDownLatch(1)
        val release = CountDownLatch(1)

        @Volatile private var blockNext = false

        @Volatile var dueIds: List<UUID> = emptyList()

        @Volatile var blockInFindById = false
        val findByIdCalls = AtomicInteger()
        val findByIdEntered = CountDownLatch(1)

        fun blockNextTick() {
            blockNext = true
        }

        override fun findDue(
            now: Instant,
            limit: Int,
        ): List<UUID> {
            findDueCalls.incrementAndGet()
            if (blockNext) {
                tickEntered.countDown()
                release.await(10, TimeUnit.SECONDS)
            }
            steps += "tick done"
            return dueIds
        }

        override fun findById(id: UUID): Transfer? {
            findByIdCalls.incrementAndGet()
            if (blockInFindById) {
                findByIdEntered.countDown()
                release.await(10, TimeUnit.SECONDS)
            }
            return null
        }

        override fun findByIdempotencyKey(key: String): Transfer? = unused()

        override fun insert(transfer: Transfer): Boolean = unused()

        override fun tryAcquireLease(
            id: UUID,
            owner: String,
            until: Instant,
            now: Instant,
        ): Boolean = false

        override fun saveIfOwner(
            transfer: Transfer,
            owner: String,
            nextAttemptAt: Instant?,
            leaseUntil: Instant,
            now: Instant,
        ): Boolean = unused()

        override fun releaseLease(
            id: UUID,
            owner: String,
        ) = unused()

        private fun unused(): Nothing = error("not used by the scheduler tests")
    }

    private companion object {
        val clock: Clock = Clock.fixed(Instant.parse("2026-10-27T10:00:00Z"), ZoneOffset.UTC)

        fun resumeDueOver(repository: TransferRepository): ResumeDueTransfersUseCase {
            val settings = SagaSettings()
            val unusedGateway =
                object : AccountsGateway {
                    override fun lookupAccount(id: UUID): AccountLookup = error("unused")

                    override fun perform(call: AccountsCall): CallResult = error("unused")
                }
            val tx =
                object : TransactionRunner {
                    override fun <T> inTransaction(block: () -> T): T = block()
                }
            val runSaga =
                RunTransferSagaUseCase(
                    transfers = repository,
                    stepLog =
                        object : TransferStepLog {
                            override fun append(record: StepRecord) = error("unused")
                        },
                    accounts = unusedGateway,
                    outbox =
                        object : EventOutbox {
                            override fun append(event: TransferEvent) = error("unused")
                        },
                    tx = tx,
                    backoff = RetryBackoff(Duration.ofSeconds(1), Duration.ofSeconds(15)),
                    settings = settings,
                    clock = clock,
                )
            val correlation =
                object : CorrelationContext {
                    override fun <T> withCorrelationId(
                        correlationId: String?,
                        block: () -> T,
                    ): T = block()
                }
            return ResumeDueTransfersUseCase(repository, runSaga, settings, clock, correlation) { "scheduler-test" }
        }
    }
}
