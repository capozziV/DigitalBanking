package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.RetryBackoff
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeAccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeAccountsGateway.Companion.created
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeEventOutbox
import io.github.capozziv.digitalbanking.transfers.application.fake.ImmediateTransactionRunner
import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryTransferRepository
import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryTransferStepLog
import io.github.capozziv.digitalbanking.transfers.application.fake.MutableClock
import io.github.capozziv.digitalbanking.transfers.application.fake.RecordingCorrelationContext
import io.github.capozziv.digitalbanking.transfers.domain.AccountsCall
import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import io.github.capozziv.digitalbanking.transfers.domain.TransferStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID

class ResumeDueTransfersUseCaseTest {
    private val start = Instant.parse("2026-10-27T12:00:00Z")
    private val clock = MutableClock(start)
    private val repository = InMemoryTransferRepository()
    private val tx = ImmediateTransactionRunner()
    private val accounts = FakeAccountsGateway(tx)
    private val settings = SagaSettings(dueBatchSize = 2)
    private val correlation = RecordingCorrelationContext()
    private var ownersCreated = 0
    private val runSaga =
        RunTransferSagaUseCase(
            transfers = repository,
            stepLog = InMemoryTransferStepLog(),
            accounts = accounts,
            outbox = FakeEventOutbox(),
            tx = tx,
            backoff = RetryBackoff(Duration.ofSeconds(1), Duration.ofSeconds(15)),
            settings = settings,
            clock = clock,
        )
    private val useCase =
        ResumeDueTransfersUseCase(repository, runSaga, settings, clock, correlation) { "scheduler-${++ownersCreated}" }

    private fun pending(
        n: Int,
        correlationId: String? = null,
    ): Transfer =
        Transfer
            .request(
                UUID.fromString("00000000-0000-7000-8000-00000000010$n"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Money(100),
                null,
                "key-$n",
                correlationId,
                start,
                Duration.ofMinutes(2),
            ).also { repository.insert(it) }

    @Test
    fun `runs the saga for due transfers and counts them`() {
        val first = pending(1)
        accounts.reply(created, created)

        assertThat(useCase.execute()).isEqualTo(1)

        assertThat(repository.findById(first.id)!!.status).isEqualTo(TransferStatus.COMPLETED)
    }

    @Test
    fun `skips transfers leased by another owner`() {
        val leased = pending(1)
        val free = pending(2)
        repository.holdLease(leased.id, "other-instance", start.plusSeconds(30))
        accounts.reply(created, created)

        assertThat(useCase.execute()).isEqualTo(1)

        assertThat(repository.findById(leased.id)!!.status).isEqualTo(TransferStatus.PENDING)
        assertThat(repository.findById(free.id)!!.status).isEqualTo(TransferStatus.COMPLETED)
    }

    @Test
    fun `takes at most the batch size`() {
        pending(1)
        pending(2)
        pending(3)
        accounts.reply(created, created, created, created)

        assertThat(useCase.execute()).isEqualTo(2)
    }

    @Test
    fun `ignores transfers not yet due`() {
        val transfer = pending(1)
        repository.tryAcquireLease(transfer.id, "x", start.plusSeconds(1), start)
        repository.saveIfOwner(transfer, "x", start.plusSeconds(60), start.plusSeconds(1))
        repository.releaseLease(transfer.id, "x")

        assertThat(useCase.execute()).isEqualTo(0)
        assertThat(accounts.calls).isEmpty()
    }

    @Test
    fun `returns zero when nothing is due`() {
        assertThat(useCase.execute()).isEqualTo(0)
    }

    @Test
    fun `an exception in the saga of one transfer does not stop the batch`() {
        val broken = pending(1)
        val healthy = pending(2)
        accounts.onCall = { call -> check(call.accountId() != broken.sourceAccountId) { "boom" } }
        accounts.reply(created, created)

        assertThat(useCase.execute()).isEqualTo(1)

        assertThat(repository.findById(broken.id)!!.status).isEqualTo(TransferStatus.PENDING)
        assertThat(repository.leaseOwner(broken.id)).isNull()
        assertThat(repository.findById(healthy.id)!!.status).isEqualTo(TransferStatus.COMPLETED)
    }

    @Test
    fun `each saga run takes the lease under a new owner`() {
        val first = pending(1)
        val second = pending(2)
        val owners = mutableListOf<String?>()
        accounts.onCall = { call ->
            val transferId = if (call.accountId() in first.accountIds()) first.id else second.id
            owners += repository.leaseOwner(transferId)
        }
        accounts.reply(created, created, created, created)

        assertThat(useCase.execute()).isEqualTo(2)

        assertThat(owners).containsExactly("scheduler-1", "scheduler-1", "scheduler-2", "scheduler-2")
    }

    @Test
    fun `the saga of each transfer runs under its own correlation id`() {
        pending(1, correlationId = "corr-a")
        pending(2, correlationId = null)
        val seen = mutableListOf<String?>()
        accounts.onCall = { seen += correlation.current }
        accounts.reply(created, created, created, created)

        useCase.execute()

        assertThat(seen).containsExactly("corr-a", "corr-a", null, null)
        assertThat(correlation.current).isNull()
    }

    @Test
    fun `stops the batch when shouldContinue turns false`() {
        val first = pending(1)
        val second = pending(2)
        accounts.reply(created, created, created, created)
        var checks = 0

        assertThat(useCase.execute { ++checks <= 1 }).isEqualTo(1)

        assertThat(repository.findById(first.id)!!.status).isEqualTo(TransferStatus.COMPLETED)
        assertThat(repository.findById(second.id)!!.status).isEqualTo(TransferStatus.PENDING)
        assertThat(repository.leaseOwner(second.id)).isNull()
    }

    @Test
    fun `an interruption ends the batch and restores the interrupt flag`() {
        val first = pending(1)
        val second = pending(2)
        accounts.onCall = { throw InterruptedException("shutdown") }
        accounts.reply(created, created, created, created)

        try {
            assertThat(useCase.execute()).isEqualTo(0)
            assertThat(Thread.currentThread().isInterrupted).isTrue()
        } finally {
            Thread.interrupted()
        }

        assertThat(repository.findById(first.id)!!.status).isEqualTo(TransferStatus.PENDING)
        assertThat(repository.findById(second.id)!!.status).isEqualTo(TransferStatus.PENDING)
        assertThat(accounts.calls).hasSize(1)
    }

    private fun Transfer.accountIds() = setOf(sourceAccountId, destinationAccountId)

    private fun AccountsCall.accountId(): UUID =
        when (this) {
            is AccountsCall.Debit -> accountId
            is AccountsCall.Credit -> accountId
            is AccountsCall.FindEntry -> accountId
        }
}
