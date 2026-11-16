package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.fake.CallLog
import io.github.capozziv.digitalbanking.accounts.application.fake.DirectTransactionRunner
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryLedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeLimit
import io.github.capozziv.digitalbanking.accounts.domain.AvailableBalance
import io.github.capozziv.digitalbanking.accounts.domain.Balance
import io.github.capozziv.digitalbanking.accounts.domain.EntryKind
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
import io.github.capozziv.digitalbanking.accounts.domain.Money
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import io.github.capozziv.digitalbanking.security.Caller
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class GetAccountUseCaseTest {
    private val accountId = UUID.fromString("0199a0b0-0000-7000-8000-0000000000a1")
    private val customerId = UUID.fromString("0199a0b0-0000-7000-8000-0000000000c1")
    private val account = Account(accountId, customerId, AccountType.STANDARD)
    private val accounts = InMemoryAccountRepository()
    private val entries = InMemoryLedgerEntryRepository()
    private val types =
        InMemoryAccountTypeRepository(
            listOf(
                AccountTypeLimit(AccountType.FREE, OverdraftLimit(0), Instant.parse("2026-10-01T00:00:00Z")),
                AccountTypeLimit(AccountType.STANDARD, OverdraftLimit(50000), Instant.parse("2026-10-01T00:00:00Z")),
            ),
        )
    private val tx = DirectTransactionRunner()
    private val useCase = GetAccountUseCase(accounts, entries, types, tx)
    private val owner = Caller.Customer(customerId)

    private fun entry(
        sequence: Long,
        kind: EntryKind,
        amount: Long,
        balanceAfter: Long,
    ) = LedgerEntry(
        id = UUID.fromString("0199a0b0-0000-7000-8000-%012d".format(100 + sequence)),
        accountId = accountId,
        sequence = sequence,
        kind = kind,
        amount = Money(amount),
        balanceAfter = Balance(balanceAfter),
        description = null,
        idempotencyKey = "key-$sequence",
        createdAt = Instant.parse("2026-10-22T12:00:00Z").plusSeconds(sequence),
    )

    @Test
    fun `standard account without entries has zero balance and the overdraft limit as available balance`() {
        accounts.insert(account, "open-key-1")

        val view = useCase.execute(accountId, owner)

        assertThat(view).isEqualTo(
            AccountView(account, Balance(0), OverdraftLimit(50000), AvailableBalance(50000)),
        )
    }

    @Test
    fun `balance comes from the last entry and available balance adds the overdraft limit`() {
        accounts.insert(account, "open-key-1")
        entries.insert(entry(1, EntryKind.CREDIT, 10000, 10000))
        entries.insert(entry(2, EntryKind.DEBIT, 30000, -20000))

        val view = useCase.execute(accountId, owner)

        assertThat(view).isEqualTo(
            AccountView(account, Balance(-20000), OverdraftLimit(50000), AvailableBalance(30000)),
        )
    }

    @Test
    fun `account, balance and overdraft limit are read in one read-only transaction`() {
        accounts.insert(account, "open-key-1")
        val log = CallLog()
        val tracked = GetAccountUseCase(log.accounts(accounts), log.entries(entries), log.types(types), log.transactions(tx))

        tracked.execute(accountId, owner)

        assertThat(log.calls).containsExactly(
            "begin read-only",
            "accounts.findById",
            "entries.findLast",
            "types.findLimit",
            "end",
        )
        assertThat(tx.transactions).isZero()
    }

    @Test
    fun `the account of another customer fails with account not found, so its existence is not revealed`() {
        accounts.insert(account, "open-key-1")
        val stranger = Caller.Customer(UUID.fromString("0199a0b0-0000-7000-8000-0000000000c2"))

        assertThatThrownBy { useCase.execute(accountId, stranger) }
            .isInstanceOfSatisfying(AccountNotFoundException::class.java) {
                assertThat(it.accountId).isEqualTo(accountId)
            }
    }

    @Test
    fun `an admin reads the account of any customer`() {
        accounts.insert(account, "open-key-1")

        val view = useCase.execute(accountId, Caller.Admin(UUID.fromString("0199a0b0-0000-7000-8000-0000000000ad")))

        assertThat(view.account).isEqualTo(account)
    }

    @Test
    fun `an admin who is the customer reads its own account`() {
        accounts.insert(account, "open-key-1")

        assertThat(useCase.execute(accountId, Caller.Admin(customerId)).account).isEqualTo(account)
    }

    @Test
    fun `a service reads any account, with its customer id`() {
        accounts.insert(account, "open-key-1")

        assertThat(useCase.execute(accountId, Caller.Service).account.customerId).isEqualTo(customerId)
    }

    @Test
    fun `unknown account fails with account not found`() {
        assertThatThrownBy { useCase.execute(accountId, owner) }
            .isInstanceOfSatisfying(AccountNotFoundException::class.java) {
                assertThat(it.accountId).isEqualTo(accountId)
            }
    }
}
