package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.IdempotencyKeyReusedException
import io.github.capozziv.digitalbanking.transfers.application.RetryBackoff
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeAccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeAccountsGateway.Companion.created
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeEventOutbox
import io.github.capozziv.digitalbanking.transfers.application.fake.ImmediateTransactionRunner
import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryTransferRepository
import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryTransferStepLog
import io.github.capozziv.digitalbanking.transfers.application.fake.MutableClock
import io.github.capozziv.digitalbanking.transfers.application.port.AccountLookup
import io.github.capozziv.digitalbanking.transfers.application.port.IdGenerator
import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import io.github.capozziv.digitalbanking.transfers.domain.TransferStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID

class RequestTransferUseCaseTest {
    private val newId = UUID.fromString("00000000-0000-7000-8000-000000000001")
    private val source = UUID.fromString("00000000-0000-7000-8000-0000000000a1")
    private val destination = UUID.fromString("00000000-0000-7000-8000-0000000000b2")
    private val start = Instant.parse("2026-10-27T12:00:00Z")

    private val clock = MutableClock(start)
    private val repository = InMemoryTransferRepository()
    private val tx = ImmediateTransactionRunner()
    private val accounts = FakeAccountsGateway(tx)
    private val settings = SagaSettings()
    private val ids =
        object : IdGenerator {
            override fun newId(): UUID = newId
        }
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
    private val useCase = RequestTransferUseCase(repository, accounts, runSaga, ids, settings, clock)

    private fun command(
        sourceId: UUID = source,
        destinationId: UUID = destination,
        amount: Long = 500,
        description: String? = "rent",
    ) = RequestTransferCommand(sourceId, destinationId, Money(amount), description, "key-1", "corr-1")

    private fun existingTransfer(
        id: UUID = UUID.fromString("00000000-0000-7000-8000-0000000000ee"),
        description: String? = "rent",
    ) = Transfer.request(id, source, destination, Money(500), description, "key-1", "corr-0", start, settings.deadline)

    @Test
    fun `new request runs the saga and returns Created with the final state`() {
        accounts.reply(created, created)

        val outcome = useCase.execute(command())

        val transfer = (outcome as RequestTransferOutcome.Created).transfer
        assertThat(transfer.id).isEqualTo(newId)
        assertThat(transfer.status).isEqualTo(TransferStatus.COMPLETED)
        assertThat(transfer.correlationId).isEqualTo("corr-1")
        assertThat(transfer.deadline).isEqualTo(start.plus(settings.deadline))
        assertThat(repository.findById(newId)!!.status).isEqualTo(TransferStatus.COMPLETED)
        assertThat(repository.leaseOwner(newId)).isNull()
    }

    @Test
    fun `same source and destination is rejected without calling accounts`() {
        val outcome = useCase.execute(command(destinationId = source))

        assertThat(outcome).isEqualTo(RequestTransferOutcome.SameAccount)
        assertThat(accounts.lookups).isEmpty()
        assertThat(accounts.calls).isEmpty()
        assertThat(repository.findById(newId)).isNull()
    }

    @Test
    fun `missing destination returns AccountNotFound and writes nothing`() {
        accounts.lookupResults[destination] = AccountLookup.NOT_FOUND

        val outcome = useCase.execute(command())

        assertThat(outcome).isEqualTo(RequestTransferOutcome.AccountNotFound(destination))
        assertThat(repository.findById(newId)).isNull()
        assertThat(accounts.calls).isEmpty()
    }

    @Test
    fun `missing source returns AccountNotFound for the source`() {
        accounts.lookupResults[source] = AccountLookup.NOT_FOUND

        assertThat(useCase.execute(command())).isEqualTo(RequestTransferOutcome.AccountNotFound(source))
    }

    @Test
    fun `unavailable accounts returns AccountsUnavailable and writes nothing`() {
        accounts.lookupResults[source] = AccountLookup.UNAVAILABLE

        val outcome = useCase.execute(command())

        assertThat(outcome).isEqualTo(RequestTransferOutcome.AccountsUnavailable)
        assertThat(repository.findById(newId)).isNull()
    }

    @Test
    fun `repeat with a different description is Existing and does not look up or run the saga`() {
        val existing = existingTransfer(description = "rent")
        repository.insert(existing)

        val outcome = useCase.execute(command(description = "other"))

        assertThat(outcome).isEqualTo(RequestTransferOutcome.Existing(existing))
        assertThat(accounts.lookups).isEmpty()
        assertThat(accounts.calls).isEmpty()
    }

    @Test
    fun `repeat with a different amount throws IdempotencyKeyReusedException`() {
        repository.insert(existingTransfer())

        assertThatThrownBy { useCase.execute(command(amount = 999)) }
            .isInstanceOf(IdempotencyKeyReusedException::class.java)
    }

    @Test
    fun `repeat with a different destination throws IdempotencyKeyReusedException`() {
        repository.insert(existingTransfer())

        assertThatThrownBy { useCase.execute(command(destinationId = UUID.randomUUID())) }
            .isInstanceOf(IdempotencyKeyReusedException::class.java)
    }

    @Test
    fun `insert race is treated as a repeat and returns Existing`() {
        val winner = existingTransfer(id = UUID.fromString("00000000-0000-7000-8000-0000000000ff"))
        val racingRepository =
            object : TransferRepository by repository {
                private var inserted = false

                override fun findByIdempotencyKey(key: String): Transfer? = if (inserted) repository.findByIdempotencyKey(key) else null

                override fun insert(transfer: Transfer): Boolean {
                    repository.insert(winner)
                    inserted = true
                    return repository.insert(transfer)
                }
            }
        val racing = RequestTransferUseCase(racingRepository, accounts, runSaga, ids, settings, clock)

        val outcome = racing.execute(command())

        assertThat(outcome).isEqualTo(RequestTransferOutcome.Existing(winner))
    }

    @Test
    fun `when the lease is held by another run the persisted state is returned`() {
        repository.holdLease(newId, "other", Instant.MAX)

        val outcome = useCase.execute(command())

        val transfer = (outcome as RequestTransferOutcome.Created).transfer
        assertThat(transfer.id).isEqualTo(newId)
        assertThat(transfer.status).isEqualTo(TransferStatus.PENDING)
        assertThat(accounts.calls).isEmpty()
    }
}
