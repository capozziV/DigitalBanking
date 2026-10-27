package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.fake.FakeAccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeAccountsGateway.Companion.created
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeEventOutbox
import io.github.capozziv.digitalbanking.transfers.application.fake.ImmediateTransactionRunner
import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryTransferRepository
import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryTransferStepLog
import io.github.capozziv.digitalbanking.transfers.application.fake.MutableClock
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
    private val useCase = ResumeDueTransfersUseCase(repository, runSaga, settings, clock, "scheduler-1")

    private fun pending(n: Int): Transfer =
        Transfer
            .request(
                UUID.fromString("00000000-0000-7000-8000-00000000010$n"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Money(100),
                null,
                "key-$n",
                null,
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
}
