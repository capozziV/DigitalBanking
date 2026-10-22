package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountBusyException
import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.fake.CallLog
import io.github.capozziv.digitalbanking.accounts.application.fake.DirectTransactionRunner
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.RecordingEventOutbox
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeChanged
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class ChangeAccountTypeUseCaseTest {
    private val account =
        Account(
            id = UUID.fromString("0199a0b0-0000-7000-8000-0000000000a1"),
            customerId = UUID.fromString("0199a0b0-0000-7000-8000-0000000000c1"),
            type = AccountType.FREE,
        )
    private val accounts = InMemoryAccountRepository().apply { insert(account, "open-key-1") }
    private val outbox = RecordingEventOutbox()
    private val tx = DirectTransactionRunner()
    private val useCase = ChangeAccountTypeUseCase(accounts, outbox, tx)

    @Test
    fun `upgrade updates the type and appends account type changed to the outbox`() {
        val result = useCase.execute(account.id, AccountType.GOLD)

        val upgraded = account.copy(type = AccountType.GOLD)
        assertThat(result).isEqualTo(upgraded)
        assertThat(accounts.findById(account.id)).isEqualTo(upgraded)
        assertThat(outbox.events).containsExactly(
            AccountTypeChanged(accountId = account.id, previousType = AccountType.FREE, newType = AccountType.GOLD),
        )
        assertThat(tx.transactions).isEqualTo(1)
    }

    @Test
    fun `downgrade updates the type and appends account type changed to the outbox`() {
        useCase.execute(account.id, AccountType.DIAMOND)

        val result = useCase.execute(account.id, AccountType.STANDARD)

        val downgraded = account.copy(type = AccountType.STANDARD)
        assertThat(result).isEqualTo(downgraded)
        assertThat(accounts.findById(account.id)).isEqualTo(downgraded)
        assertThat(outbox.events).containsExactly(
            AccountTypeChanged(accountId = account.id, previousType = AccountType.FREE, newType = AccountType.DIAMOND),
            AccountTypeChanged(accountId = account.id, previousType = AccountType.DIAMOND, newType = AccountType.STANDARD),
        )
    }

    @Test
    fun `changing to the same type returns the account without update or event`() {
        val log = CallLog()
        val tracked = ChangeAccountTypeUseCase(log.accounts(accounts), log.outbox(outbox), log.transactions(tx))

        val result = tracked.execute(account.id, AccountType.FREE)

        assertThat(result).isEqualTo(account)
        assertThat(accounts.findById(account.id)).isEqualTo(account)
        assertThat(outbox.events).isEmpty()
        assertThat(log.calls).containsExactly("begin", "accounts.lockById", "end")
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
    fun `type change follows lock, type update and event append in one transaction`() {
        val log = CallLog()
        val tracked = ChangeAccountTypeUseCase(log.accounts(accounts), log.outbox(outbox), log.transactions(tx))

        tracked.execute(account.id, AccountType.BLACK)

        assertThat(log.calls).containsExactly(
            "begin",
            "accounts.lockById",
            "accounts.updateType",
            "outbox.append",
            "end",
        )
    }
}
