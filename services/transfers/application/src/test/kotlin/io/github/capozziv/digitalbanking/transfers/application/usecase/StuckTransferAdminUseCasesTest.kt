package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.RetryBackoff
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeAccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeAccountsGateway.Companion.created
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeAccountsGateway.Companion.refused
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeEventOutbox
import io.github.capozziv.digitalbanking.transfers.application.fake.ImmediateTransactionRunner
import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryDailyLimitGateway
import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryManualResolutionLog
import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryTransferRepository
import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryTransferStepLog
import io.github.capozziv.digitalbanking.transfers.application.fake.MutableClock
import io.github.capozziv.digitalbanking.transfers.application.fake.RecordingCorrelationContext
import io.github.capozziv.digitalbanking.transfers.application.port.CorrelationContext
import io.github.capozziv.digitalbanking.transfers.application.port.EventOutbox
import io.github.capozziv.digitalbanking.transfers.application.port.ManualResolution
import io.github.capozziv.digitalbanking.transfers.application.port.RefusedCompensation
import io.github.capozziv.digitalbanking.transfers.application.port.StuckTransferFinder
import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.StepCall
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import io.github.capozziv.digitalbanking.transfers.domain.TransferEvent
import io.github.capozziv.digitalbanking.transfers.domain.TransferStatus
import io.github.capozziv.digitalbanking.transfers.domain.TransferStep
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** The admin's way out of a compensation `accounts` keeps refusing: list, retry now, or compensate by hand. */
class StuckTransferAdminUseCasesTest {
    private val id = UUID.fromString("00000000-0000-7000-8000-000000000001")
    private val source = UUID.fromString("00000000-0000-7000-8000-0000000000a1")
    private val destination = UUID.fromString("00000000-0000-7000-8000-0000000000b2")
    private val admin = UUID.fromString("00000000-0000-7000-8000-0000000000ad")
    private val start = Instant.parse("2026-10-27T12:00:00Z")

    private val clock = MutableClock(start)
    private val repository = InMemoryTransferRepository()
    private val stepLog = InMemoryTransferStepLog()
    private val outbox = FakeEventOutbox()
    private val tx = ImmediateTransactionRunner()
    private val accounts = FakeAccountsGateway(tx)
    private val dailyLimits = InMemoryDailyLimitGateway(transactions = tx)
    private val resolutions = InMemoryManualResolutionLog()
    private val correlation = RecordingCorrelationContext()
    private val settings = SagaSettings()
    private var owners = 0
    private val newOwner = { "admin-${++owners}" }
    private val runSaga =
        RunTransferSagaUseCase(
            repository,
            stepLog,
            accounts,
            dailyLimits,
            outbox,
            tx,
            RetryBackoff(Duration.ofSeconds(1), Duration.ofSeconds(15)),
            settings,
            clock,
        )
    private val retry = RetryTransferUseCase(repository, runSaga, correlation, newOwner)
    private val compensate =
        CompensateTransferManuallyUseCase(repository, resolutions, outbox, dailyLimits, tx, settings, correlation, clock, newOwner)

    private fun givenTransfer(
        step: TransferStep = TransferStep.Compensate,
        limitReserved: Boolean = true,
    ): Transfer {
        val transfer =
            Transfer
                .request(
                    id = id,
                    sourceAccountId = source,
                    sourceCustomerId = FakeAccountsGateway.OWNER,
                    destinationAccountId = destination,
                    amount = Money(500),
                    description = "rent",
                    idempotencyKey = "key-1",
                    correlationId = "corr-1",
                    now = start,
                    deadlineAfter = Duration.ofMinutes(2),
                ).copy(step = step, attempts = 4, limitReserved = limitReserved)
        repository.insert(transfer)
        if (limitReserved) dailyLimits.reserve(id, source, transfer.limitDay, transfer.amount)
        return transfer
    }

    // --- list ---

    @Test
    fun `lists the refused compensations with their transfer, in the finder's order`() {
        givenTransfer()
        val refusal = RefusedCompensation(id, "account-closed", start.plusSeconds(30))
        val missing = RefusedCompensation(UUID.randomUUID(), "x", start)
        val finder =
            object : StuckTransferFinder {
                override fun findRefusedCompensations(limit: Int) = listOf(refusal, missing).take(limit)
            }

        val stuck = ListStuckTransfersUseCase(finder, repository).execute(limit = 10)

        assertThat(stuck).containsExactly(StuckTransfer(repository.findById(id)!!, "account-closed", start.plusSeconds(30)))
    }

    // --- retry now ---

    @Test
    fun `retrying runs the compensation now under the transfer's correlation id and ends compensated when accepted`() {
        givenTransfer()
        var correlationOfCall: String? = null
        accounts.reply(created)
        val tracking =
            object : CorrelationContext {
                override fun <T> withCorrelationId(
                    correlationId: String?,
                    block: () -> T,
                ): T =
                    correlation.withCorrelationId(correlationId) {
                        correlationOfCall = correlation.current
                        block()
                    }
            }

        val outcome = RetryTransferUseCase(repository, runSaga, tracking, newOwner).execute(id)

        assertThat(outcome).isInstanceOf(RetryTransferOutcome.Ran::class.java)
        assertThat((outcome as RetryTransferOutcome.Ran).transfer.status).isEqualTo(TransferStatus.COMPENSATED)
        assertThat(accounts.calls.single()).isInstanceOf(StepCall.Credit::class.java)
        assertThat(correlationOfCall).isEqualTo("corr-1")
        assertThat(outbox.events).containsExactly(TransferEvent.TransferCompensated(id, source, destination, Money(500), "rent"))
    }

    @Test
    fun `retrying a compensation refused again leaves it pending in compensate`() {
        givenTransfer()
        accounts.reply(refused("account-closed"))

        val outcome = retry.execute(id)

        assertThat((outcome as RetryTransferOutcome.Ran).transfer.step).isEqualTo(TransferStep.Compensate)
        assertThat(outbox.events).isEmpty()
    }

    @Test
    fun `retrying an unknown transfer is not found, a finished one is not pending, a leased one is busy`() {
        assertThat(retry.execute(id)).isEqualTo(RetryTransferOutcome.NotFound)

        val done = givenTransfer(TransferStep.Completed, limitReserved = false)
        assertThat(retry.execute(id)).isEqualTo(RetryTransferOutcome.NotPending(done))
        assertThat(accounts.calls).isEmpty()
    }

    @Test
    fun `retrying a transfer whose lease another run holds is busy and calls nobody`() {
        givenTransfer()
        repository.holdLease(id, "scheduler-run", start.plusSeconds(30))

        assertThat(retry.execute(id)).isEqualTo(RetryTransferOutcome.Busy)
        assertThat(accounts.calls).isEmpty()
    }

    // --- compensate by hand ---

    @Test
    fun `compensating by hand ends compensated, records who and why, emits the event and releases the limit`() {
        givenTransfer()
        clock.advance(Duration.ofHours(1))

        val outcome = compensate.execute(id, admin, "credited back by the back office, ticket 42")

        val resolution = ManualResolution(id, admin, "credited back by the back office, ticket 42", clock.instant())
        val saved = repository.findById(id)!!
        assertThat(outcome).isEqualTo(CompensateManuallyOutcome.Compensated(saved, resolution, repeated = false))
        assertThat(saved.status).isEqualTo(TransferStatus.COMPENSATED)
        assertThat(saved.updatedAt).isEqualTo(clock.instant())
        assertThat(resolutions.find(id)).isEqualTo(resolution)
        assertThat(outbox.events).containsExactly(TransferEvent.TransferCompensated(id, source, destination, Money(500), "rent"))
        assertThat(repository.nextAttemptAt(id)).isNull()
        assertThat(repository.leaseOwner(id)).isNull()
        assertThat(dailyLimits.releaseCalls).containsExactly(id)
        assertThat(accounts.calls).isEmpty()
    }

    @Test
    fun `the event of a manual compensation carries the transfer's correlation id, not the request's`() {
        givenTransfer()
        val correlationsAtAppend = mutableListOf<String?>()
        val recordingOutbox =
            object : EventOutbox {
                override fun append(event: TransferEvent) {
                    correlationsAtAppend += correlation.current
                }
            }
        val useCase =
            CompensateTransferManuallyUseCase(
                repository,
                resolutions,
                recordingOutbox,
                dailyLimits,
                tx,
                settings,
                correlation,
                clock,
                newOwner,
            )

        correlation.withCorrelationId("admin-request") { useCase.execute(id, admin, "why") }

        assertThat(correlationsAtAppend).containsExactly("corr-1")
    }

    @Test
    fun `compensating by hand again answers the first resolution and writes nothing more`() {
        givenTransfer()
        val first = compensate.execute(id, admin, "first") as CompensateManuallyOutcome.Compensated
        clock.advance(Duration.ofMinutes(5))

        val again = compensate.execute(id, UUID.randomUUID(), "second")

        assertThat(again).isEqualTo(first.copy(repeated = true))
        assertThat(outbox.events).hasSize(1)
        assertThat(resolutions.resolutions).hasSize(1)
    }

    @Test
    fun `only a transfer in compensate can be compensated by hand`() {
        val credit = givenTransfer(TransferStep.Credit)

        assertThat(compensate.execute(id, admin, "why")).isEqualTo(CompensateManuallyOutcome.NotCompensating(credit))
        assertThat(resolutions.resolutions).isEmpty()
        assertThat(outbox.events).isEmpty()
    }

    @Test
    fun `a transfer compensated by the saga is not compensating, not a repeat`() {
        val done = givenTransfer(TransferStep.Compensated)

        assertThat(compensate.execute(id, admin, "why")).isEqualTo(CompensateManuallyOutcome.NotCompensating(done))
    }

    @Test
    fun `compensating by hand an unknown transfer is not found`() {
        assertThat(compensate.execute(id, admin, "why")).isEqualTo(CompensateManuallyOutcome.NotFound)
    }

    @Test
    fun `compensating by hand while a saga run holds the lease is busy and writes nothing`() {
        givenTransfer()
        repository.holdLease(id, "scheduler-run", start.plusSeconds(30))

        assertThat(compensate.execute(id, admin, "why")).isEqualTo(CompensateManuallyOutcome.Busy)
        assertThat(repository.findById(id)!!.step).isEqualTo(TransferStep.Compensate)
        assertThat(resolutions.resolutions).isEmpty()
        assertThat(outbox.events).isEmpty()
        assertThat(repository.leaseOwner(id)).isEqualTo("scheduler-run")
    }

    @Test
    fun `a transfer that never reserved the limit releases nothing`() {
        givenTransfer(limitReserved = false)

        compensate.execute(id, admin, "why")

        assertThat(dailyLimits.releaseCalls).isEmpty()
    }

    @Test
    fun `a failing release of the limit does not undo the resolution`() {
        givenTransfer()
        dailyLimits.failOnRelease = true

        val outcome = compensate.execute(id, admin, "why")

        assertThat(outcome).isInstanceOf(CompensateManuallyOutcome.Compensated::class.java)
        assertThat(repository.findById(id)!!.status).isEqualTo(TransferStatus.COMPENSATED)
    }
}
