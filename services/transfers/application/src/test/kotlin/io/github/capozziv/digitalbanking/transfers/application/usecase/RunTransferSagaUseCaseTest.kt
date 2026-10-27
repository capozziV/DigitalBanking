package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.fake.FakeAccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeAccountsGateway.Companion.busy
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeAccountsGateway.Companion.created
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeAccountsGateway.Companion.refused
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeAccountsGateway.Companion.timeout
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeEventOutbox
import io.github.capozziv.digitalbanking.transfers.application.fake.ImmediateTransactionRunner
import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryTransferRepository
import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryTransferStepLog
import io.github.capozziv.digitalbanking.transfers.application.fake.MutableClock
import io.github.capozziv.digitalbanking.transfers.domain.AccountsCall
import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.StepOutcome
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import io.github.capozziv.digitalbanking.transfers.domain.TransferEvent
import io.github.capozziv.digitalbanking.transfers.domain.TransferStatus
import io.github.capozziv.digitalbanking.transfers.domain.TransferStep
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID

class RunTransferSagaUseCaseTest {
    private val id = UUID.fromString("00000000-0000-7000-8000-000000000001")
    private val source = UUID.fromString("00000000-0000-7000-8000-0000000000a1")
    private val destination = UUID.fromString("00000000-0000-7000-8000-0000000000b2")
    private val start = Instant.parse("2026-10-27T12:00:00Z")

    private val clock = MutableClock(start)
    private val repository = InMemoryTransferRepository()
    private val stepLog = InMemoryTransferStepLog()
    private val outbox = FakeEventOutbox()
    private val tx = ImmediateTransactionRunner()
    private val accounts = FakeAccountsGateway(tx)
    private val useCase =
        RunTransferSagaUseCase(
            transfers = repository,
            stepLog = stepLog,
            accounts = accounts,
            outbox = outbox,
            tx = tx,
            backoff = RetryBackoff(Duration.ofSeconds(1), Duration.ofSeconds(15)),
            settings = SagaSettings(),
            clock = clock,
        )

    private fun givenTransfer(step: TransferStep = TransferStep.Debit): Transfer {
        val transfer =
            Transfer
                .request(
                    id = id,
                    sourceAccountId = source,
                    destinationAccountId = destination,
                    amount = Money(500),
                    description = "rent",
                    idempotencyKey = "key-1",
                    correlationId = "corr-1",
                    now = start,
                    deadlineAfter = Duration.ofMinutes(2),
                ).copy(step = step)
        repository.insert(transfer)
        return transfer
    }

    @Test
    fun `happy path completes with two step records and one completed event`() {
        givenTransfer()
        accounts.reply(created, created)

        val result = useCase.execute(id, "owner-a")

        assertThat(result!!.status).isEqualTo(TransferStatus.COMPLETED)
        assertThat(stepLog.records.map { it.step }).containsExactly("DEBIT", "CREDIT")
        assertThat(stepLog.records.map { it.outcome }).containsExactly("SUCCESS", "SUCCESS")
        assertThat(stepLog.records.map { it.httpStatus }).containsExactly(201, 201)
        assertThat(outbox.events).containsExactly(TransferEvent.TransferCompleted(id, source, destination, Money(500), "rent"))
        assertThat(repository.findById(id)!!.status).isEqualTo(TransferStatus.COMPLETED)
        assertThat(repository.nextAttemptAt(id)).isNull()
        assertThat(repository.leaseOwner(id)).isNull()
    }

    @Test
    fun `sends the exact keys and descriptions to accounts`() {
        givenTransfer()
        accounts.reply(created, created)

        useCase.execute(id, "owner-a")

        assertThat(accounts.calls)
            .containsExactly(
                AccountsCall.Debit(source, Money(500), "transfer-$id-debit", "Transfer to $destination"),
                AccountsCall.Credit(destination, Money(500), "transfer-$id-credit", "Transfer from $source"),
            )
    }

    @Test
    fun `sends the compensation key and description`() {
        givenTransfer()
        accounts.reply(created, refused("account-not-found"), created)

        val result = useCase.execute(id, "owner-a")

        assertThat(result!!.status).isEqualTo(TransferStatus.COMPENSATED)
        assertThat(accounts.calls.last())
            .isEqualTo(AccountsCall.Credit(source, Money(500), "transfer-$id-compensation", "Transfer $id compensation"))
        assertThat(outbox.events).containsExactly(TransferEvent.TransferCompensated(id, source, destination, Money(500), "rent"))
    }

    @Test
    fun `refused debit fails the transfer with the reason`() {
        givenTransfer()
        accounts.reply(refused(StepOutcome.INSUFFICIENT_AVAILABLE_BALANCE))

        val result = useCase.execute(id, "owner-a")

        assertThat(result!!.status).isEqualTo(TransferStatus.FAILED)
        assertThat(accounts.calls).hasSize(1)
        assertThat(outbox.events)
            .containsExactly(
                TransferEvent.TransferFailed(id, source, destination, Money(500), "rent", StepOutcome.INSUFFICIENT_AVAILABLE_BALANCE),
            )
    }

    @Test
    fun `entry not found while resolving the debit fails with debit-not-confirmed`() {
        givenTransfer(TransferStep.ResolveDebit)
        accounts.reply(refused(StepOutcome.ENTRY_NOT_FOUND))

        val result = useCase.execute(id, "owner-a")

        assertThat(result!!.status).isEqualTo(TransferStatus.FAILED)
        assertThat(result.failureReason).isEqualTo("debit-not-confirmed")
        assertThat((outbox.events.single() as TransferEvent.TransferFailed).reason).isEqualTo("debit-not-confirmed")
    }

    @Test
    fun `busy twice then created completes without scheduling a retry`() {
        givenTransfer()
        accounts.reply(busy, busy, created, created)

        val result = useCase.execute(id, "owner-a", busyAttempts = 3)

        assertThat(result!!.status).isEqualTo(TransferStatus.COMPLETED)
        assertThat(repository.nextAttemptAt(id)).isNull()
        assertThat(accounts.calls).hasSize(4)
        assertThat(stepLog.records.map { it.httpStatus }).containsExactly(503, 503, 201, 201)
        assertThat(result.attempts).isZero()
    }

    @Test
    fun `three busy attempts in total count one attempt and schedule a retry`() {
        givenTransfer()
        accounts.reply(busy, busy, busy)

        val result = useCase.execute(id, "owner-a", busyAttempts = 3)

        assertThat(result!!.step).isEqualTo(TransferStep.Debit)
        assertThat(result.attempts).isEqualTo(1)
        assertThat(accounts.calls).hasSize(3)
        assertThat(stepLog.records.map { it.httpStatus }).containsExactly(503, 503, 503)
        assertThat(repository.nextAttemptAt(id)).isEqualTo(start.plusSeconds(1))
    }

    @Test
    fun `busy step records are not written when the lease is lost`() {
        givenTransfer()
        accounts.reply(busy, created)
        repository.loseLeaseOnSave(1)

        val result = useCase.execute(id, "owner-a", busyAttempts = 3)

        assertThat(result).isNull()
        assertThat(accounts.calls).hasSize(2)
        assertThat(stepLog.records).isEmpty()
    }

    @Test
    fun `a step that continues right away keeps the transfer due so it resumes after a crash`() {
        givenTransfer()
        accounts.reply(created)
        repository.failOnRelease = true

        assertThatThrownBy { useCase.execute(id, "owner-a") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("no scripted reply")

        assertThat(repository.findById(id)!!.step).isEqualTo(TransferStep.Credit)
        assertThat(repository.nextAttemptAt(id)).isEqualTo(start)
        assertThat(repository.findDue(start.plusSeconds(10), 50)).isEmpty()
        assertThat(repository.findDue(start.plus(SagaSettings().lease).plusSeconds(1), 50)).containsExactly(id)
    }

    @Test
    fun `each saved step renews the lease`() {
        givenTransfer()
        accounts.reply(created, created)
        accounts.onCall = { clock.advance(Duration.ofSeconds(5)) }

        useCase.execute(id, "owner-a")

        assertThat(repository.renewedLeases).containsExactly(start.plusSeconds(35), start.plusSeconds(40))
    }

    @Test
    fun `stops before a call that could outlive the lease and writes nothing`() {
        givenTransfer()
        accounts.reply(busy, busy, busy)
        accounts.onCall = { clock.advance(Duration.ofSeconds(13)) }

        val result = useCase.execute(id, "owner-a", busyAttempts = 3)

        assertThat(accounts.calls).hasSize(2)
        assertThat(result!!.step).isEqualTo(TransferStep.Debit)
        assertThat(result.attempts).isZero()
        assertThat(repository.saveCount).isZero()
        assertThat(stepLog.records).isEmpty()
        assertThat(repository.leaseOwner(id)).isNull()
    }

    @Test
    fun `renewed lease lets the next step run`() {
        givenTransfer()
        accounts.reply(created, created)
        // 27 s: past the first lease's call budget (30 s - 4 s), so the credit only runs if the debit save renewed it.
        accounts.onCall = { clock.advance(Duration.ofSeconds(27)) }

        val result = useCase.execute(id, "owner-a")

        assertThat(result!!.status).isEqualTo(TransferStatus.COMPLETED)
        assertThat(accounts.calls).hasSize(2)
    }

    @Test
    fun `busy is not retried immediately by default`() {
        givenTransfer()
        accounts.reply(busy)

        val result = useCase.execute(id, "owner-a")

        assertThat(accounts.calls).hasSize(1)
        assertThat(result!!.attempts).isEqualTo(1)
        assertThat(repository.nextAttemptAt(id)).isEqualTo(start.plusSeconds(1))
    }

    @Test
    fun `timeout on credit stays pending, schedules one second later and releases the lease`() {
        givenTransfer()
        accounts.reply(created, timeout)

        val result = useCase.execute(id, "owner-a")

        assertThat(result!!.status).isEqualTo(TransferStatus.PENDING)
        assertThat(result.step).isEqualTo(TransferStep.Credit)
        assertThat(result.attempts).isEqualTo(1)
        assertThat(repository.nextAttemptAt(id)).isEqualTo(start.plusSeconds(1))
        assertThat(repository.leaseOwner(id)).isNull()
        assertThat(outbox.events).isEmpty()
        assertThat(stepLog.records.last().httpStatus).isNull()
        assertThat(stepLog.records.last().outcome).isEqualTo("TRANSIENT_FAILURE")
    }

    @Test
    fun `backoff grows with consecutive failures of the same step`() {
        givenTransfer()
        accounts.reply(timeout, timeout, timeout)

        useCase.execute(id, "owner-a")
        useCase.execute(id, "owner-a")
        useCase.execute(id, "owner-a")

        assertThat(repository.findById(id)!!.attempts).isEqualTo(3)
        assertThat(repository.nextAttemptAt(id)).isEqualTo(start.plusSeconds(4))
    }

    @Test
    fun `timeout past the deadline moves to resolve and stops until the next attempt`() {
        givenTransfer()
        clock.advance(Duration.ofMinutes(3))
        accounts.reply(timeout)

        val result = useCase.execute(id, "owner-a")

        assertThat(result!!.step).isEqualTo(TransferStep.ResolveDebit)
        assertThat(accounts.calls).hasSize(1)
        assertThat(repository.nextAttemptAt(id)).isEqualTo(clock.instant().plusSeconds(1))
    }

    @Test
    fun `resolve step refused for another reason stays in the step and does not spin`() {
        givenTransfer(TransferStep.ResolveDebit)
        accounts.reply(refused("something-else"))

        val result = useCase.execute(id, "owner-a")

        assertThat(result!!.step).isEqualTo(TransferStep.ResolveDebit)
        assertThat(accounts.calls).hasSize(1)
        assertThat(repository.nextAttemptAt(id)).isEqualTo(start.plusSeconds(1))
        assertThat(outbox.events).isEmpty()
    }

    @Test
    fun `refused compensation stays in compensate and is scheduled after the maximum wait`() {
        givenTransfer(TransferStep.Compensate)
        accounts.reply(refused("account-not-found"))

        val result = useCase.execute(id, "owner-a")

        assertThat(result!!.step).isEqualTo(TransferStep.Compensate)
        assertThat(repository.nextAttemptAt(id)).isEqualTo(start.plusSeconds(15))
    }

    @Test
    fun `transient failure in compensation keeps the normal backoff`() {
        givenTransfer(TransferStep.Compensate)
        accounts.reply(timeout)

        useCase.execute(id, "owner-a")

        assertThat(repository.nextAttemptAt(id)).isEqualTo(start.plusSeconds(1))
    }

    @Test
    fun `busy lease returns null and calls nothing`() {
        givenTransfer()
        repository.holdLease(id, "owner-b", start.plusSeconds(30))

        val result = useCase.execute(id, "owner-a")

        assertThat(result).isNull()
        assertThat(accounts.calls).isEmpty()
        assertThat(stepLog.records).isEmpty()
        assertThat(repository.leaseOwner(id)).isEqualTo("owner-b")
    }

    @Test
    fun `expired lease of another owner can be taken over`() {
        givenTransfer()
        repository.holdLease(id, "owner-b", start.minusSeconds(1))
        accounts.reply(created, created)

        val result = useCase.execute(id, "owner-a")

        assertThat(result!!.status).isEqualTo(TransferStatus.COMPLETED)
    }

    @Test
    fun `lost lease stops the loop and writes no event`() {
        givenTransfer()
        accounts.reply(created, created)
        repository.loseLeaseOnSave(2)

        val result = useCase.execute(id, "owner-a")

        assertThat(result).isNull()
        assertThat(accounts.calls).hasSize(2)
        assertThat(outbox.events).isEmpty()
        assertThat(repository.findById(id)!!.step).isEqualTo(TransferStep.Credit)
        assertThat(stepLog.records.map { it.step }).containsExactly("DEBIT")
        assertThat(repository.leaseOwner(id)).isEqualTo("someone-else")
    }

    @Test
    fun `lease is released when the gateway throws`() {
        givenTransfer()

        assertThatThrownBy { useCase.execute(id, "owner-a") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("no scripted reply")

        assertThat(repository.leaseOwner(id)).isNull()
    }

    @Test
    fun `a failing lease release does not hide the original exception`() {
        givenTransfer()
        repository.failOnRelease = true

        assertThatThrownBy { useCase.execute(id, "owner-a") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("no scripted reply")
            .hasNoSuppressedExceptions()
    }

    @Test
    fun `unknown transfer returns null`() {
        assertThat(useCase.execute(UUID.randomUUID(), "owner-a")).isNull()
    }

    @Test
    fun `terminal transfer makes no call`() {
        givenTransfer(TransferStep.Completed)

        val result = useCase.execute(id, "owner-a")

        assertThat(result!!.status).isEqualTo(TransferStatus.COMPLETED)
        assertThat(accounts.calls).isEmpty()
    }
}
