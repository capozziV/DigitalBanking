package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.security.Caller
import io.github.capozziv.digitalbanking.transfers.application.IdempotencyKeyReusedException
import io.github.capozziv.digitalbanking.transfers.application.RetryBackoff
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeAccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeAccountsGateway.Companion.created
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeCustomerGateway
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeEventOutbox
import io.github.capozziv.digitalbanking.transfers.application.fake.ImmediateTransactionRunner
import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryDailyLimitGateway
import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryTransferRepository
import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryTransferStepLog
import io.github.capozziv.digitalbanking.transfers.application.fake.MutableClock
import io.github.capozziv.digitalbanking.transfers.application.port.AccountLookup
import io.github.capozziv.digitalbanking.transfers.application.port.CustomerLookup
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
    private val owner = FakeAccountsGateway.OWNER
    private val otherCustomer = UUID.fromString("00000000-0000-7000-8000-0000000000d4")
    private val start = Instant.parse("2026-10-27T12:00:00Z")

    private val clock = MutableClock(start)
    private val repository = InMemoryTransferRepository()
    private val tx = ImmediateTransactionRunner()
    private val accounts = FakeAccountsGateway(tx)
    private val customers = FakeCustomerGateway()
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
            dailyLimits = InMemoryDailyLimitGateway(transactions = tx),
            outbox = FakeEventOutbox(),
            tx = tx,
            backoff = RetryBackoff(Duration.ofSeconds(1), Duration.ofSeconds(15)),
            settings = settings,
            clock = clock,
        )
    private val useCase = RequestTransferUseCase(repository, accounts, customers, runSaga, ids, settings, clock)

    private fun command(
        sourceId: UUID = source,
        destinationId: UUID = destination,
        amount: Long = 500,
        description: String? = "rent",
        caller: Caller = Caller.Customer(owner),
    ) = RequestTransferCommand(sourceId, destinationId, Money(amount), description, "key-1", "corr-1", caller)

    private fun existingTransfer(
        id: UUID = UUID.fromString("00000000-0000-7000-8000-0000000000ee"),
        description: String? = "rent",
        amount: Long = 500,
    ) = Transfer.request(id, source, owner, destination, Money(amount), description, "key-1", "corr-0", start, settings.deadline)

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
    fun `the transfer records the customer that owns the source account`() {
        accounts.reply(created, created)

        useCase.execute(command())

        assertThat(repository.findById(newId)!!.sourceCustomerId).isEqualTo(owner)
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
        accounts.lookupResults[destination] = AccountLookup.NotFound

        val outcome = useCase.execute(command())

        assertThat(outcome).isEqualTo(RequestTransferOutcome.AccountNotFound(destination))
        assertThat(repository.findById(newId)).isNull()
        assertThat(accounts.calls).isEmpty()
    }

    @Test
    fun `a destination of another customer is accepted`() {
        accounts.lookupResults[destination] = AccountLookup.Found(otherCustomer)
        accounts.reply(created, created)

        val outcome = useCase.execute(command())

        assertThat((outcome as RequestTransferOutcome.Created).transfer.status).isEqualTo(TransferStatus.COMPLETED)
    }

    @Test
    fun `missing source returns SourceAccountNotFound`() {
        accounts.lookupResults[source] = AccountLookup.NotFound

        assertThat(useCase.execute(command())).isEqualTo(RequestTransferOutcome.SourceAccountNotFound(source))
    }

    @Test
    fun `a source of another customer is answered as not found and nothing is written`() {
        accounts.lookupResults[source] = AccountLookup.Found(otherCustomer)

        val outcome = useCase.execute(command())

        assertThat(outcome).isEqualTo(RequestTransferOutcome.SourceAccountNotFound(source))
        assertThat(repository.findById(newId)).isNull()
        assertThat(customers.lookups).isEmpty()
        assertThat(accounts.calls).isEmpty()
    }

    @Test
    fun `a service caller cannot request a transfer`() {
        assertThat(useCase.execute(command(caller = Caller.Service))).isEqualTo(RequestTransferOutcome.SourceAccountNotFound(source))
        assertThat(repository.findById(newId)).isNull()
    }

    @Test
    fun `an admin may transfer from the account of any customer, checking that customer`() {
        accounts.lookupResults[source] = AccountLookup.Found(otherCustomer)
        accounts.reply(created, created)

        val outcome = useCase.execute(command(caller = Caller.Admin(UUID.randomUUID())))

        assertThat((outcome as RequestTransferOutcome.Created).transfer.sourceCustomerId).isEqualTo(otherCustomer)
        assertThat(customers.lookups).containsExactly(otherCustomer)
    }

    @Test
    fun `an admin that is also the customer transfers from its own account`() {
        accounts.reply(created, created)

        val outcome = useCase.execute(command(caller = Caller.Admin(owner)))

        assertThat((outcome as RequestTransferOutcome.Created).transfer.sourceCustomerId).isEqualTo(owner)
    }

    @Test
    fun `the customer is looked up before the transfer is created`() {
        accounts.reply(created, created)

        useCase.execute(command())

        assertThat(customers.lookups).containsExactly(owner)
    }

    @Test
    fun `a blocked customer returns CustomerBlocked and writes nothing`() {
        customers.result = CustomerLookup.BLOCKED

        val outcome = useCase.execute(command())

        assertThat(outcome).isEqualTo(RequestTransferOutcome.CustomerBlocked)
        assertThat(repository.findById(newId)).isNull()
        assertThat(accounts.calls).isEmpty()
    }

    @Test
    fun `an unknown customer returns CustomerNotFound and writes nothing`() {
        customers.result = CustomerLookup.NOT_FOUND

        assertThat(useCase.execute(command())).isEqualTo(RequestTransferOutcome.CustomerNotFound)
        assertThat(repository.findById(newId)).isNull()
    }

    @Test
    fun `customer unavailable returns CustomersUnavailable and writes nothing`() {
        customers.result = CustomerLookup.UNAVAILABLE

        assertThat(useCase.execute(command())).isEqualTo(RequestTransferOutcome.CustomersUnavailable)
        assertThat(repository.findById(newId)).isNull()
        assertThat(accounts.calls).isEmpty()
    }

    @Test
    fun `unavailable accounts returns AccountsUnavailable and writes nothing`() {
        accounts.lookupResults[source] = AccountLookup.Unavailable

        val outcome = useCase.execute(command())

        assertThat(outcome).isEqualTo(RequestTransferOutcome.AccountsUnavailable)
        assertThat(repository.findById(newId)).isNull()
        assertThat(customers.lookups).isEmpty()
    }

    @Test
    fun `repeat with a different description is Existing and does not look up or run the saga`() {
        val existing = existingTransfer(description = "rent")
        repository.insert(existing)

        val outcome = useCase.execute(command(description = "other"))

        assertThat(outcome).isEqualTo(RequestTransferOutcome.Existing(existing))
        assertThat(accounts.lookups).isEmpty()
        assertThat(customers.lookups).isEmpty()
        assertThat(accounts.calls).isEmpty()
    }

    @Test
    fun `repeat of another customer's transfer is answered as not found`() {
        repository.insert(existingTransfer())

        val outcome = useCase.execute(command(caller = Caller.Customer(otherCustomer)))

        assertThat(outcome).isEqualTo(RequestTransferOutcome.SourceAccountNotFound(source))
    }

    @Test
    fun `repeat by an admin is Existing`() {
        val existing = existingTransfer()
        repository.insert(existing)

        assertThat(useCase.execute(command(caller = Caller.Admin(UUID.randomUUID())))).isEqualTo(RequestTransferOutcome.Existing(existing))
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
        val racing = RequestTransferUseCase(racingRepository(winner), accounts, customers, runSaga, ids, settings, clock)

        val outcome = racing.execute(command())

        assertThat(outcome).isEqualTo(RequestTransferOutcome.Existing(winner))
    }

    @Test
    fun `insert race with a different request throws IdempotencyKeyReusedException`() {
        val winner = existingTransfer(id = UUID.fromString("00000000-0000-7000-8000-0000000000ff"), amount = 999)
        val racing = RequestTransferUseCase(racingRepository(winner), accounts, customers, runSaga, ids, settings, clock)

        assertThatThrownBy { racing.execute(command(amount = 500)) }
            .isInstanceOf(IdempotencyKeyReusedException::class.java)
        assertThat(accounts.calls).isEmpty()
    }

    @Test
    fun `busyAttempts from the settings reaches the saga`() {
        val twoAttempts = SagaSettings(busyAttempts = 2)
        val configured = RequestTransferUseCase(repository, accounts, customers, runSaga, ids, twoAttempts, clock)
        accounts.reply(FakeAccountsGateway.busy, FakeAccountsGateway.busy)

        val outcome = configured.execute(command())

        assertThat(accounts.calls).hasSize(2)
        assertThat((outcome as RequestTransferOutcome.Created).transfer.status).isEqualTo(TransferStatus.PENDING)
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

    /** A repository where [winner] is inserted by a concurrent request just before this one. */
    private fun racingRepository(winner: Transfer) =
        object : TransferRepository by repository {
            private var inserted = false

            override fun findByIdempotencyKey(key: String): Transfer? = if (inserted) repository.findByIdempotencyKey(key) else null

            override fun insert(transfer: Transfer): Boolean {
                repository.insert(winner)
                inserted = true
                return repository.insert(transfer)
            }
        }
}
