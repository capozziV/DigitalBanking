package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountBusyException
import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.fake.CallLog
import io.github.capozziv.digitalbanking.accounts.application.fake.DirectTransactionRunner
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryLedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.RecordingEventOutbox
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeChanged
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

class ChangeAccountTypeUseCaseTest {
    private val account =
        Account(
            id = UUID.fromString("0199a0b0-0000-7000-8000-0000000000a1"),
            customerId = UUID.fromString("0199a0b0-0000-7000-8000-0000000000c1"),
            type = AccountType.FREE,
        )
    private val accounts = InMemoryAccountRepository().apply { insert(account, "open-key-1") }
    private val entries = InMemoryLedgerEntryRepository()
    private val outbox = RecordingEventOutbox()
    private val tx = DirectTransactionRunner()
    private val types =
        InMemoryAccountTypeRepository(
            AccountType.entries.mapIndexed { index, type ->
                AccountTypeLimit(type, OverdraftLimit((index + 1) * 10_000L), Instant.parse("2026-10-22T00:00:00Z"))
            },
        )
    private val useCase = ChangeAccountTypeUseCase(accounts, entries, types, outbox, tx)

    private fun limitOf(type: AccountType) = types.findLimit(type)

    private fun viewOf(
        account: Account,
        balance: Balance = Balance.ZERO,
    ): AccountView {
        val limit = limitOf(account.type)
        return AccountView(account, balance, limit, AvailableBalance.of(balance, limit))
    }

    private fun tracked(log: CallLog) =
        ChangeAccountTypeUseCase(log.accounts(accounts), log.entries(entries), log.types(types), log.outbox(outbox), log.transactions(tx))

    @Test
    fun `upgrade updates the type and appends account type changed to the outbox`() {
        val result = useCase.execute(account.id, AccountType.GOLD)

        val upgraded = account.copy(type = AccountType.GOLD)
        assertThat(result).isEqualTo(viewOf(upgraded))
        assertThat(accounts.findById(account.id)).isEqualTo(upgraded)
        assertThat(outbox.events).containsExactly(
            AccountTypeChanged(
                accountId = account.id,
                previousType = AccountType.FREE,
                newType = AccountType.GOLD,
                overdraftLimit = limitOf(AccountType.GOLD),
            ),
        )
        assertThat(tx.transactions).isEqualTo(1)
    }

    @Test
    fun `downgrade updates the type and appends account type changed to the outbox`() {
        useCase.execute(account.id, AccountType.DIAMOND)

        val result = useCase.execute(account.id, AccountType.STANDARD)

        val downgraded = account.copy(type = AccountType.STANDARD)
        assertThat(result).isEqualTo(viewOf(downgraded))
        assertThat(accounts.findById(account.id)).isEqualTo(downgraded)
        assertThat(outbox.events).containsExactly(
            AccountTypeChanged(
                accountId = account.id,
                previousType = AccountType.FREE,
                newType = AccountType.DIAMOND,
                overdraftLimit = limitOf(AccountType.DIAMOND),
            ),
            AccountTypeChanged(
                accountId = account.id,
                previousType = AccountType.DIAMOND,
                newType = AccountType.STANDARD,
                overdraftLimit = limitOf(AccountType.STANDARD),
            ),
        )
    }

    @Test
    fun `the returned view carries the balance and the new type's available balance`() {
        entries.insert(
            LedgerEntry(
                id = UUID.fromString("0199a0b0-0000-7000-8000-000000000101"),
                accountId = account.id,
                sequence = 1,
                kind = EntryKind.CREDIT,
                amount = Money(2_500),
                balanceAfter = Balance(2_500),
                description = null,
                idempotencyKey = "credit-key-1",
                createdAt = Instant.parse("2026-10-22T12:00:00Z"),
            ),
        )

        val result = useCase.execute(account.id, AccountType.GOLD)

        assertThat(result).isEqualTo(viewOf(account.copy(type = AccountType.GOLD), Balance(2_500)))
    }

    @Test
    fun `changing to the same type returns the account view without update or event`() {
        val log = CallLog()

        val result = tracked(log).execute(account.id, AccountType.FREE)

        assertThat(result).isEqualTo(viewOf(account))
        assertThat(accounts.findById(account.id)).isEqualTo(account)
        assertThat(outbox.events).isEmpty()
        assertThat(log.calls).containsExactly("begin", "accounts.lockById", "types.findLimit", "entries.findLast", "end")
    }

    @Test
    fun `changing the type of an unknown account fails with account not found`() {
        val unknown = UUID.fromString("0199a0b0-0000-7000-8000-0000000000a9")

        assertThatThrownBy { useCase.execute(unknown, AccountType.GOLD) }
            .isInstanceOfSatisfying(AccountNotFoundException::class.java) {
                assertThat(it.accountId).isEqualTo(unknown)
                assertThat(it).hasMessage("account $unknown not found")
            }
        assertThat(accounts.all()).containsExactly(account)
        assertThat(outbox.events).isEmpty()
    }

    @Test
    fun `changing the type of a busy account propagates account busy without update or event`() {
        accounts.busyAccounts += account.id

        assertThatThrownBy { useCase.execute(account.id, AccountType.GOLD) }
            .isInstanceOfSatisfying(AccountBusyException::class.java) {
                assertThat(it.accountId).isEqualTo(account.id)
                assertThat(it).hasMessage("account ${account.id} is busy")
            }
        assertThat(accounts.findById(account.id)).isEqualTo(account)
        assertThat(outbox.events).isEmpty()
    }

    @Test
    fun `type change follows lock, type update, event append and the response read in one transaction`() {
        val log = CallLog()

        tracked(log).execute(account.id, AccountType.BLACK)

        assertThat(log.calls).containsExactly(
            "begin",
            "accounts.lockById",
            "types.findLimit",
            "accounts.updateType",
            "outbox.append",
            "entries.findLast",
            "end",
        )
    }
}
