package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
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
    private val useCase = GetAccountUseCase(accounts, entries, types)

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

        val view = useCase.execute(accountId)

        assertThat(view).isEqualTo(
            AccountView(account, Balance(0), OverdraftLimit(50000), AvailableBalance(50000)),
        )
    }

    @Test
    fun `balance comes from the last entry and available balance adds the overdraft limit`() {
        accounts.insert(account, "open-key-1")
        entries.insert(entry(1, EntryKind.CREDIT, 10000, 10000))
        entries.insert(entry(2, EntryKind.DEBIT, 30000, -20000))

        val view = useCase.execute(accountId)

        assertThat(view).isEqualTo(
            AccountView(account, Balance(-20000), OverdraftLimit(50000), AvailableBalance(30000)),
        )
    }

    @Test
    fun `unknown account fails with account not found`() {
        assertThatThrownBy { useCase.execute(accountId) }
            .isInstanceOfSatisfying(AccountNotFoundException::class.java) {
                assertThat(it.accountId).isEqualTo(accountId)
            }
    }
}
