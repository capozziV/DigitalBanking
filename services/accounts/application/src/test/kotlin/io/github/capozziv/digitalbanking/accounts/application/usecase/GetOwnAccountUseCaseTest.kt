package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.CallerNotCustomerException
import io.github.capozziv.digitalbanking.accounts.application.NoOpenAccountException
import io.github.capozziv.digitalbanking.accounts.application.fake.DirectTransactionRunner
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryLedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountStatus
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeLimit
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

class GetOwnAccountUseCaseTest {
    private val customerId = UUID.fromString("0199a0b0-0000-7000-8000-0000000000c1")
    private val account = Account(UUID.fromString("0199a0b0-0000-7000-8000-0000000000a1"), customerId, AccountType.FREE)
    private val accounts = InMemoryAccountRepository()
    private val entries = InMemoryLedgerEntryRepository()
    private val types =
        InMemoryAccountTypeRepository(
            AccountType.entries.map { AccountTypeLimit(it, OverdraftLimit(0), Instant.parse("2026-10-01T00:00:00Z")) },
        )
    private val useCase = GetOwnAccountUseCase(accounts, entries, types, DirectTransactionRunner())

    @Test
    fun `returns the not closed account of the caller with its balance`() {
        accounts.insert(account, "open-1")
        entries.insert(
            LedgerEntry(
                UUID.randomUUID(),
                account.id,
                1,
                EntryKind.CREDIT,
                Money(10_000),
                Balance(10_000),
                null,
                "k1",
                Instant.parse("2026-11-19T10:00:00Z"),
            ),
        )

        val view = useCase.execute(Caller.Customer(customerId))

        assertThat(view.account).isEqualTo(account)
        assertThat(view.balance).isEqualTo(Balance(10_000))
    }

    @Test
    fun `an admin reads the account of its own user`() {
        accounts.insert(account, "open-1")

        assertThat(useCase.execute(Caller.Admin(customerId)).account).isEqualTo(account)
    }

    @Test
    fun `fails with no open account when the caller has none`() {
        assertThatThrownBy { useCase.execute(Caller.Customer(customerId)) }
            .isInstanceOfSatisfying(NoOpenAccountException::class.java) { assertThat(it.customerId).isEqualTo(customerId) }
    }

    @Test
    fun `ignores a closed account`() {
        accounts.insert(account.copy(status = AccountStatus.CLOSED), "open-1")

        assertThatThrownBy { useCase.execute(Caller.Customer(customerId)) }.isInstanceOf(NoOpenAccountException::class.java)
    }

    @Test
    fun `a blocked account still counts`() {
        accounts.insert(account.copy(status = AccountStatus.BLOCKED), "open-1")

        assertThat(useCase.execute(Caller.Customer(customerId)).account.status).isEqualTo(AccountStatus.BLOCKED)
    }

    @Test
    fun `a service has no account of its own`() {
        assertThatThrownBy { useCase.execute(Caller.Service) }.isInstanceOf(CallerNotCustomerException::class.java)
    }
}
