package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.CustomerAlreadyHasAccountException
import io.github.capozziv.digitalbanking.accounts.application.IdempotencyKeyReusedException
import io.github.capozziv.digitalbanking.accounts.application.fake.CallLog
import io.github.capozziv.digitalbanking.accounts.application.fake.DirectTransactionRunner
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryLedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.RecordingEventOutbox
import io.github.capozziv.digitalbanking.accounts.application.fake.SequentialIdGenerator
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountOpened
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeLimit
import io.github.capozziv.digitalbanking.accounts.domain.AvailableBalance
import io.github.capozziv.digitalbanking.accounts.domain.Balance
import io.github.capozziv.digitalbanking.accounts.domain.EntryKind
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
import io.github.capozziv.digitalbanking.accounts.domain.Money
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
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
    private val useCase = OpenAccountUseCase(accounts, entries, types, outbox, tx, SequentialIdGenerator())

    private val expectedAccount = Account(SequentialIdGenerator.idAt(1), customerId, AccountType.FREE)
    private val expectedView = AccountView(expectedAccount, Balance.ZERO, OverdraftLimit(1_000), AvailableBalance(1_000))

    private fun tracked(log: CallLog) =
        OpenAccountUseCase(
            log.accounts(accounts),
            log.entries(entries),
            log.types(types),
            log.outbox(outbox),
            log.transactions(tx),
            SequentialIdGenerator(),
        )

    @Test
    fun `opening a new account creates a free account and appends account opened to the outbox`() {
        val result = useCase.execute(OpenAccountCommand(customerId, OPENING_KEY))

        assertThat(result).isEqualTo(OpenAccountResult(expectedView, created = true))
        assertThat(accounts.all()).containsExactly(expectedAccount)
        assertThat(outbox.events).containsExactly(AccountOpened(expectedAccount.id, customerId, AccountType.FREE))
        assertThat(tx.transactions).isEqualTo(1)
    }

    @Test
    fun `retrying with the same opening key returns the same account without a new event`() {
        useCase.execute(OpenAccountCommand(customerId, OPENING_KEY))

        val retry = useCase.execute(OpenAccountCommand(customerId, OPENING_KEY))

        assertThat(retry).isEqualTo(OpenAccountResult(expectedView, created = false))
        assertThat(accounts.all()).containsExactly(expectedAccount)
        assertThat(outbox.events).containsExactly(AccountOpened(expectedAccount.id, customerId, AccountType.FREE))
    }

    @Test
    fun `the response of a new account is read in the opening transaction`() {
        val log = CallLog()
        val tracked = tracked(log)

        tracked.execute(OpenAccountCommand(customerId, OPENING_KEY))

        assertThat(log.calls).containsExactly("begin", "outbox.append", "types.findLimit", "end")
    }

    @Test
    fun `a retry returns the current balance of the existing account read in the same transaction`() {
        useCase.execute(OpenAccountCommand(customerId, OPENING_KEY))
        entries.insert(
            LedgerEntry(
                id = UUID.fromString("0199a0b0-0000-7000-8000-000000000101"),
                accountId = expectedAccount.id,
                sequence = 1,
                kind = EntryKind.CREDIT,
                amount = Money(4_000),
                balanceAfter = Balance(4_000),
                description = null,
                idempotencyKey = "credit-key-1",
                createdAt = Instant.parse("2026-10-22T12:00:00Z"),
            ),
        )
        val log = CallLog()
        val tracked = tracked(log)

        val retry = tracked.execute(OpenAccountCommand(customerId, OPENING_KEY))

        assertThat(retry).isEqualTo(
            OpenAccountResult(
                AccountView(expectedAccount, Balance(4_000), OverdraftLimit(1_000), AvailableBalance(5_000)),
                created = false,
            ),
        )
        assertThat(log.calls).containsExactly("begin", "entries.findLast", "types.findLimit", "end")
    }

    @Test
    fun `reusing an opening key of another customer fails without returning that account`() {
        val otherCustomerId = UUID.fromString("0199a0b0-0000-7000-8000-0000000000c2")
        useCase.execute(OpenAccountCommand(customerId, OPENING_KEY))

        assertThatThrownBy { useCase.execute(OpenAccountCommand(otherCustomerId, OPENING_KEY)) }
            .isInstanceOfSatisfying(IdempotencyKeyReusedException::class.java) {
                assertThat(it.key).isEqualTo(OPENING_KEY)
            }
        assertThat(accounts.all()).containsExactly(expectedAccount)
        assertThat(outbox.events).containsExactly(AccountOpened(expectedAccount.id, customerId, AccountType.FREE))
    }

    @Test
    fun `opening a second account for the same customer with another key fails`() {
        useCase.execute(OpenAccountCommand(customerId, OPENING_KEY))

        assertThatThrownBy { useCase.execute(OpenAccountCommand(customerId, "open-key-2")) }
            .isInstanceOfSatisfying(CustomerAlreadyHasAccountException::class.java) {
                assertThat(it.customerId).isEqualTo(customerId)
            }
        assertThat(accounts.all()).containsExactly(expectedAccount)
        assertThat(outbox.events).containsExactly(AccountOpened(expectedAccount.id, customerId, AccountType.FREE))
    }

    private companion object {
        const val OPENING_KEY = "open-key-1"
    }
}
