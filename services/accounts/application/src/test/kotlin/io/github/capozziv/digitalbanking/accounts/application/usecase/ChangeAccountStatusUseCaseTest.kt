package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.fake.CallLog
import io.github.capozziv.digitalbanking.accounts.application.fake.DirectTransactionRunner
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryLedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.RecordingEventOutbox
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountBlocked
import io.github.capozziv.digitalbanking.accounts.domain.AccountClosedException
import io.github.capozziv.digitalbanking.accounts.domain.AccountStatus
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeLimit
import io.github.capozziv.digitalbanking.accounts.domain.AccountUnblocked
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class ChangeAccountStatusUseCaseTest {
    private val now = Instant.parse("2026-11-16T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
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
            AccountType.entries.map { AccountTypeLimit(it, OverdraftLimit(10_000), Instant.parse("2026-10-22T00:00:00Z")) },
        )
    private val useCase = ChangeAccountStatusUseCase(accounts, entries, types, outbox, tx, clock)

    @Test
    fun `blocking writes the status and appends AccountBlocked under the account lock, in one transaction`() {
        val log = CallLog()
        val tracked =
            ChangeAccountStatusUseCase(
                log.accounts(accounts),
                log.entries(entries),
                log.types(types),
                log.outbox(outbox),
                log.transactions(tx),
                clock,
            )

        val view = tracked.execute(account.id, AccountStatus.BLOCKED)

        assertThat(view.account.status).isEqualTo(AccountStatus.BLOCKED)
        assertThat(accounts.findById(account.id)!!.status).isEqualTo(AccountStatus.BLOCKED)
        assertThat(outbox.events).containsExactly(AccountBlocked(account.id, account.customerId, now))
        assertThat(log.calls.take(2)).containsExactly("begin", "accounts.lockById")
        assertThat(log.calls).containsSubsequence("accounts.updateStatus", "outbox.append", "end")
    }

    @Test
    fun `unblocking appends AccountUnblocked`() {
        useCase.execute(account.id, AccountStatus.BLOCKED)

        useCase.execute(account.id, AccountStatus.ACTIVE)

        assertThat(outbox.events.last()).isEqualTo(AccountUnblocked(account.id, account.customerId, now))
        assertThat(accounts.findById(account.id)!!.status).isEqualTo(AccountStatus.ACTIVE)
    }

    @Test
    fun `the same status writes nothing and appends no event`() {
        val view = useCase.execute(account.id, AccountStatus.ACTIVE)

        assertThat(view.account.status).isEqualTo(AccountStatus.ACTIVE)
        assertThat(outbox.events).isEmpty()
    }

    @Test
    fun `a closed account cannot change status`() {
        accounts.updateStatus(account.copy(status = AccountStatus.CLOSED))

        assertThatThrownBy { useCase.execute(account.id, AccountStatus.BLOCKED) }.isInstanceOf(AccountClosedException::class.java)
        assertThat(outbox.events).isEmpty()
    }

    @Test
    fun `an unknown account is AccountNotFoundException`() {
        assertThatThrownBy { useCase.execute(UUID.randomUUID(), AccountStatus.BLOCKED) }
            .isInstanceOf(AccountNotFoundException::class.java)
    }
}
