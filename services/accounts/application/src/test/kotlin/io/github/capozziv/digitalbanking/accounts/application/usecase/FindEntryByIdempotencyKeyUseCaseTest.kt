package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryLedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.Balance
import io.github.capozziv.digitalbanking.accounts.domain.EntryKind
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
import io.github.capozziv.digitalbanking.accounts.domain.Money
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class FindEntryByIdempotencyKeyUseCaseTest {
    private val accountId = UUID.fromString("0199a0b0-0000-7000-8000-0000000000a1")
    private val otherAccountId = UUID.fromString("0199a0b0-0000-7000-8000-0000000000a2")
    private val accounts = InMemoryAccountRepository()
    private val entries = InMemoryLedgerEntryRepository()
    private val useCase = FindEntryByIdempotencyKeyUseCase(accounts, entries)

    private fun entry(
        owner: UUID,
        key: String,
    ) = LedgerEntry(
        id = UUID.fromString("0199a0b0-0000-7000-8000-000000000101"),
        accountId = owner,
        sequence = 1,
        kind = EntryKind.CREDIT,
        amount = Money(1000),
        balanceAfter = Balance(1000),
        description = null,
        idempotencyKey = key,
        createdAt = Instant.parse("2026-10-27T12:00:00Z"),
    )

    @Test
    fun `returns the entry recorded with the key`() {
        accounts.insert(Account(accountId, UUID.randomUUID(), AccountType.FREE), "open-1")
        val recorded = entry(accountId, "transfer-1-debit")
        entries.insert(recorded)

        assertThat(useCase.execute(accountId, "transfer-1-debit")).isEqualTo(recorded)
    }

    @Test
    fun `returns null when the key was never used on the account`() {
        accounts.insert(Account(accountId, UUID.randomUUID(), AccountType.FREE), "open-1")

        assertThat(useCase.execute(accountId, "unknown")).isNull()
    }

    @Test
    fun `does not return an entry recorded with the same key on another account`() {
        accounts.insert(Account(accountId, UUID.randomUUID(), AccountType.FREE), "open-1")
        accounts.insert(Account(otherAccountId, UUID.randomUUID(), AccountType.FREE), "open-2")
        entries.insert(entry(otherAccountId, "shared-key"))

        assertThat(useCase.execute(accountId, "shared-key")).isNull()
    }

    @Test
    fun `fails with AccountNotFoundException when the account does not exist`() {
        assertThatThrownBy { useCase.execute(accountId, "any") }
            .isInstanceOf(AccountNotFoundException::class.java)
    }
}
