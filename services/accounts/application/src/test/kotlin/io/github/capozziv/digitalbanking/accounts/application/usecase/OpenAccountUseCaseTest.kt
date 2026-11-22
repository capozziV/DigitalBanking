package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.CallerNotCustomerException
import io.github.capozziv.digitalbanking.accounts.application.CustomerAlreadyHasAccountException
import io.github.capozziv.digitalbanking.accounts.application.CustomerBlockedException
import io.github.capozziv.digitalbanking.accounts.application.CustomerNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.CustomersUnavailableException
import io.github.capozziv.digitalbanking.accounts.application.IdempotencyKeyReusedException
import io.github.capozziv.digitalbanking.accounts.application.fake.CallLog
import io.github.capozziv.digitalbanking.accounts.application.fake.DirectTransactionRunner
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryLedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.RecordingEventOutbox
import io.github.capozziv.digitalbanking.accounts.application.fake.SequentialIdGenerator
import io.github.capozziv.digitalbanking.accounts.application.fake.StubCustomerGateway
import io.github.capozziv.digitalbanking.accounts.application.port.CustomerStatus
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountOpened
import io.github.capozziv.digitalbanking.accounts.domain.AccountStatus
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeLimit
import io.github.capozziv.digitalbanking.accounts.domain.AvailableBalance
import io.github.capozziv.digitalbanking.accounts.domain.Balance
import io.github.capozziv.digitalbanking.accounts.domain.EntryKind
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntryRecorded
import io.github.capozziv.digitalbanking.accounts.domain.Money
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import io.github.capozziv.digitalbanking.security.Caller
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class OpenAccountUseCaseTest {
    private val customerId = UUID.fromString("0199a0b0-0000-7000-8000-0000000000c1")
    private val accounts = InMemoryAccountRepository()
    private val outbox = RecordingEventOutbox()
    private val entries = InMemoryLedgerEntryRepository()
    private val types =
        InMemoryAccountTypeRepository(
            listOf(AccountTypeLimit(AccountType.FREE, OverdraftLimit(1_000), Instant.parse("2026-10-01T00:00:00Z"))),
        )
    private val tx = DirectTransactionRunner()
    private val customers = StubCustomerGateway()
    private val now = Instant.parse("2026-11-19T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val useCase =
        OpenAccountUseCase(accounts, entries, types, outbox, tx, SequentialIdGenerator(), customers, Money(BONUS_CENTS), clock)

    private val expectedAccount = Account(SequentialIdGenerator.idAt(1), customerId, AccountType.FREE)
    private val expectedView =
        AccountView(
            expectedAccount,
            Balance(BONUS_CENTS),
            OverdraftLimit(1_000),
            AvailableBalance(BONUS_CENTS + 1_000),
        )
    private val bonusEntry =
        LedgerEntry(
            id = SequentialIdGenerator.idAt(2),
            accountId = expectedAccount.id,
            sequence = 1,
            kind = EntryKind.CREDIT,
            amount = Money(BONUS_CENTS),
            balanceAfter = Balance(BONUS_CENTS),
            description = "Bônus de boas-vindas",
            idempotencyKey = "welcome-bonus:${expectedAccount.id}",
            createdAt = now,
        )
    private val expectedEvents = listOf(AccountOpened(expectedAccount.id, customerId, AccountType.FREE), LedgerEntryRecorded(bonusEntry))

    private fun tracked(log: CallLog) =
        OpenAccountUseCase(
            log.accounts(accounts),
            log.entries(entries),
            log.types(types),
            log.outbox(outbox),
            log.transactions(tx),
            SequentialIdGenerator(),
            customers,
            Money(BONUS_CENTS),
            clock,
        )

    @Test
    fun `opening an account records a welcome bonus credit of 10000 cents`() {
        val result = useCase.execute(OpenAccountCommand(Caller.Customer(customerId), OPENING_KEY))

        assertThat(result).isEqualTo(OpenAccountResult(expectedView, created = true))
        assertThat(accounts.all()).containsExactly(expectedAccount)
        assertThat(outbox.events).containsExactlyElementsOf(expectedEvents)
        assertThat(tx.transactions).isEqualTo(1)
        assertThat(entries.findLast(expectedAccount.id)).isEqualTo(bonusEntry)
    }

    @Test
    fun `retry with the same opening key does not record a second bonus`() {
        useCase.execute(OpenAccountCommand(Caller.Customer(customerId), OPENING_KEY))

        val retry = useCase.execute(OpenAccountCommand(Caller.Customer(customerId), OPENING_KEY))

        assertThat(retry.created).isFalse()
        assertThat(retry.view.balance).isEqualTo(Balance(BONUS_CENTS))
        assertThat(entries.all()).containsExactly(bonusEntry)
        assertThat(outbox.events).hasSize(2)
    }

    @Test
    fun `without a configured bonus the account starts at zero with no entry`() {
        val noBonus =
            OpenAccountUseCase(accounts, entries, types, outbox, tx, SequentialIdGenerator(), customers, welcomeBonus = null, clock = clock)

        val result = noBonus.execute(OpenAccountCommand(Caller.Customer(customerId), OPENING_KEY))

        assertThat(result.view.balance).isEqualTo(Balance.ZERO)
        assertThat(entries.all()).isEmpty()
        assertThat(outbox.events).containsExactly(AccountOpened(expectedAccount.id, customerId, AccountType.FREE))
    }

    @Test
    fun `a customer whose earlier account is closed gets no bonus on the new account`() {
        val previous = Account(UUID.fromString("0199a0b0-0000-7000-8000-0000000000a1"), customerId, AccountType.FREE, AccountStatus.CLOSED)
        accounts.insert(previous, "previous-opening-key")

        val result = useCase.execute(OpenAccountCommand(Caller.Customer(customerId), OPENING_KEY))

        assertThat(result.created).isTrue()
        assertThat(result.view.balance).isEqualTo(Balance.ZERO)
        assertThat(entries.all()).isEmpty()
        assertThat(outbox.events).containsExactly(AccountOpened(expectedAccount.id, customerId, AccountType.FREE))
    }

    @Test
    fun `the bonus of another customer does not stop the bonus of a new customer`() {
        val other = Account(UUID.fromString("0199a0b0-0000-7000-8000-0000000000a2"), UUID.randomUUID(), AccountType.FREE)
        accounts.insert(other, "other-opening-key")

        val result = useCase.execute(OpenAccountCommand(Caller.Customer(customerId), OPENING_KEY))

        assertThat(result.view.balance).isEqualTo(Balance(BONUS_CENTS))
    }

    @Test
    fun `the view of a new account shows the bonus balance`() {
        val result = useCase.execute(OpenAccountCommand(Caller.Customer(customerId), OPENING_KEY))

        assertThat(result.view.balance).isEqualTo(Balance(BONUS_CENTS))
    }

    @Test
    fun `retrying with the same opening key returns the same account without a new event`() {
        useCase.execute(OpenAccountCommand(Caller.Customer(customerId), OPENING_KEY))

        val retry = useCase.execute(OpenAccountCommand(Caller.Customer(customerId), OPENING_KEY))

        assertThat(retry).isEqualTo(OpenAccountResult(expectedView, created = false))
        assertThat(accounts.all()).containsExactly(expectedAccount)
        assertThat(outbox.events).containsExactlyElementsOf(expectedEvents)
    }

    @Test
    fun `the response of a new account is read in the opening transaction`() {
        val log = CallLog()
        val tracked = tracked(log)

        tracked.execute(OpenAccountCommand(Caller.Customer(customerId), OPENING_KEY))

        assertThat(log.calls).containsExactly("begin", "outbox.append", "entries.insert", "outbox.append", "types.findLimit", "end")
    }

    @Test
    fun `a retry returns the current balance of the existing account read in the same transaction`() {
        useCase.execute(OpenAccountCommand(Caller.Customer(customerId), OPENING_KEY))
        entries.insert(
            LedgerEntry(
                id = UUID.fromString("0199a0b0-0000-7000-8000-000000000101"),
                accountId = expectedAccount.id,
                sequence = 2,
                kind = EntryKind.CREDIT,
                amount = Money(4_000),
                balanceAfter = Balance(BONUS_CENTS + 4_000),
                description = null,
                idempotencyKey = "credit-key-1",
                createdAt = Instant.parse("2026-10-22T12:00:00Z"),
            ),
        )
        val log = CallLog()
        val tracked = tracked(log)

        val retry = tracked.execute(OpenAccountCommand(Caller.Customer(customerId), OPENING_KEY))

        assertThat(retry).isEqualTo(
            OpenAccountResult(
                AccountView(expectedAccount, Balance(BONUS_CENTS + 4_000), OverdraftLimit(1_000), AvailableBalance(BONUS_CENTS + 5_000)),
                created = false,
            ),
        )
        assertThat(log.calls).containsExactly("begin", "entries.findLast", "types.findLimit", "end")
    }

    @Test
    fun `reusing an opening key of another customer fails without returning that account`() {
        val otherCustomerId = UUID.fromString("0199a0b0-0000-7000-8000-0000000000c2")
        useCase.execute(OpenAccountCommand(Caller.Customer(customerId), OPENING_KEY))

        assertThatThrownBy { useCase.execute(OpenAccountCommand(Caller.Customer(otherCustomerId), OPENING_KEY)) }
            .isInstanceOfSatisfying(IdempotencyKeyReusedException::class.java) {
                assertThat(it.key).isEqualTo(OPENING_KEY)
            }
        assertThat(accounts.all()).containsExactly(expectedAccount)
        assertThat(outbox.events).containsExactlyElementsOf(expectedEvents)
    }

    @Test
    fun `opening a second account for the same customer with another key fails`() {
        useCase.execute(OpenAccountCommand(Caller.Customer(customerId), OPENING_KEY))

        assertThatThrownBy { useCase.execute(OpenAccountCommand(Caller.Customer(customerId), "open-key-2")) }
            .isInstanceOfSatisfying(CustomerAlreadyHasAccountException::class.java) {
                assertThat(it.customerId).isEqualTo(customerId)
            }
        assertThat(accounts.all()).containsExactly(expectedAccount)
        assertThat(outbox.events).containsExactlyElementsOf(expectedEvents)
    }

    @Test
    fun `the customer service is asked about the customer of the caller before anything is written`() {
        val log = CallLog()
        customers.log = log

        tracked(log).execute(OpenAccountCommand(Caller.Customer(customerId), OPENING_KEY))

        assertThat(customers.lookups).containsExactly(customerId)
        assertThat(
            log.calls,
        ).containsExactly("customers.findStatus", "begin", "outbox.append", "entries.insert", "outbox.append", "types.findLimit", "end")
    }

    @Test
    fun `an admin opens the account for itself, the customer whose id is its user id`() {
        val result = useCase.execute(OpenAccountCommand(Caller.Admin(customerId), OPENING_KEY))

        assertThat(result).isEqualTo(OpenAccountResult(expectedView, created = true))
        assertThat(customers.lookups).containsExactly(customerId)
    }

    @Test
    fun `a service cannot open an account, it is no customer`() {
        assertThatThrownBy { useCase.execute(OpenAccountCommand(Caller.Service, OPENING_KEY)) }
            .isInstanceOf(CallerNotCustomerException::class.java)
        assertThat(customers.lookups).isEmpty()
        assertThat(accounts.all()).isEmpty()
    }

    @Test
    fun `an unknown customer fails with customer not found and nothing is written`() {
        customers.set(customerId, null)

        assertThatThrownBy { useCase.execute(OpenAccountCommand(Caller.Customer(customerId), OPENING_KEY)) }
            .isInstanceOfSatisfying(CustomerNotFoundException::class.java) { assertThat(it.customerId).isEqualTo(customerId) }
        assertNothingWritten()
    }

    @Test
    fun `a blocked customer fails with customer blocked and nothing is written`() {
        customers.set(customerId, CustomerStatus.BLOCKED)

        assertThatThrownBy { useCase.execute(OpenAccountCommand(Caller.Customer(customerId), OPENING_KEY)) }
            .isInstanceOfSatisfying(CustomerBlockedException::class.java) { assertThat(it.customerId).isEqualTo(customerId) }
        assertNothingWritten()
    }

    @Test
    fun `a customer service failure propagates and nothing is written`() {
        customers.failFor(customerId)

        assertThatThrownBy { useCase.execute(OpenAccountCommand(Caller.Customer(customerId), OPENING_KEY)) }
            .isInstanceOf(CustomersUnavailableException::class.java)
        assertNothingWritten()
    }

    private fun assertNothingWritten() {
        assertThat(accounts.all()).isEmpty()
        assertThat(outbox.events).isEmpty()
        assertThat(tx.transactions).isZero()
    }

    private companion object {
        const val OPENING_KEY = "open-key-1"
        const val BONUS_CENTS = 10_000L
    }
}
