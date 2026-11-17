package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.PendingTransfersException
import io.github.capozziv.digitalbanking.accounts.application.TransfersUnavailableException
import io.github.capozziv.digitalbanking.accounts.application.fake.CallLog
import io.github.capozziv.digitalbanking.accounts.application.fake.DirectTransactionRunner
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryLedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.RecordingEventOutbox
import io.github.capozziv.digitalbanking.accounts.application.fake.StubTransfersGateway
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountClosed
import io.github.capozziv.digitalbanking.accounts.domain.AccountClosedException
import io.github.capozziv.digitalbanking.accounts.domain.AccountStatus
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeLimit
import io.github.capozziv.digitalbanking.accounts.domain.Balance
import io.github.capozziv.digitalbanking.accounts.domain.BalanceNotZeroException
import io.github.capozziv.digitalbanking.accounts.domain.EntryKind
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
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

class CloseAccountUseCaseTest {
    private val now = Instant.parse("2026-11-16T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val account =
        Account(
            id = UUID.fromString("0199a0b0-0000-7000-8000-0000000000a1"),
            customerId = UUID.fromString("0199a0b0-0000-7000-8000-0000000000c1"),
            type = AccountType.FREE,
        )
    private val owner = Caller.Customer(account.customerId)
    private val accounts = InMemoryAccountRepository().apply { insert(account, "open-key-1") }
    private val entries = InMemoryLedgerEntryRepository()
    private val outbox = RecordingEventOutbox()
    private val tx = DirectTransactionRunner()
    private val transfers = StubTransfersGateway()
    private val types =
        InMemoryAccountTypeRepository(
            AccountType.entries.map { AccountTypeLimit(it, OverdraftLimit(10_000), Instant.parse("2026-10-22T00:00:00Z")) },
        )
    private val useCase = CloseAccountUseCase(accounts, entries, types, outbox, tx, transfers, clock)

    private fun statusOfAccount() = accounts.findById(account.id)!!.status

    private fun credit(
        cents: Long,
        sequence: Long = 1,
    ) = entries.insert(
        LedgerEntry(
            id = UUID.randomUUID(),
            accountId = account.id,
            sequence = sequence,
            kind = EntryKind.CREDIT,
            amount = Money(cents),
            balanceAfter = Balance(cents),
            description = null,
            idempotencyKey = "credit-$sequence",
            createdAt = now,
        ),
    )

    @Test
    fun `the owner closes an account with zero balance and no pending transfer`() {
        val log = CallLog()
        val tracked =
            CloseAccountUseCase(
                log.accounts(accounts),
                log.entries(entries),
                log.types(types),
                log.outbox(outbox),
                log.transactions(tx),
                transfers,
                clock,
            )

        val view = tracked.execute(account.id, owner)

        assertThat(view.account.status).isEqualTo(AccountStatus.CLOSED)
        assertThat(view.balance).isEqualTo(Balance.ZERO)
        assertThat(statusOfAccount()).isEqualTo(AccountStatus.CLOSED)
        assertThat(outbox.events).containsExactly(AccountClosed(account.id, account.customerId, now))
        assertThat(transfers.lookups).containsExactly(account.id)
        // The final check runs under the account lock, re-reading the balance, and writes in the same transaction.
        assertThat(
            log.calls,
        ).containsSubsequence("begin", "accounts.lockById", "entries.findLast", "accounts.updateStatus", "outbox.append", "end")
    }

    @Test
    fun `an admin closes the account of any customer`() {
        useCase.execute(account.id, Caller.Admin(UUID.randomUUID()))

        assertThat(statusOfAccount()).isEqualTo(AccountStatus.CLOSED)
    }

    @Test
    fun `a pending outgoing transfer refuses the closing and leaves the account active`() {
        transfers.set(account.id, 2)

        assertThatThrownBy { useCase.execute(account.id, owner) }
            .isInstanceOf(PendingTransfersException::class.java)
            .extracting { (it as PendingTransfersException).count }
            .isEqualTo(2)
        assertThat(statusOfAccount()).isEqualTo(AccountStatus.ACTIVE)
        assertThat(outbox.events).isEmpty()
    }

    @Test
    fun `the transfers service failing is TransfersUnavailableException and leaves the account active`() {
        transfers.unavailable = true

        assertThatThrownBy { useCase.execute(account.id, owner) }.isInstanceOf(TransfersUnavailableException::class.java)
        assertThat(statusOfAccount()).isEqualTo(AccountStatus.ACTIVE)
        assertThat(outbox.events).isEmpty()
    }

    @Test
    fun `a balance other than zero is refused before asking the transfers service`() {
        credit(100)

        assertThatThrownBy { useCase.execute(account.id, owner) }.isInstanceOf(BalanceNotZeroException::class.java)
        assertThat(transfers.lookups).isEmpty()
        assertThat(statusOfAccount()).isEqualTo(AccountStatus.ACTIVE)
    }

    @Test
    fun `a credit recorded while the transfers service is asked is seen under the lock and refuses the closing`() {
        transfers.duringLookup = { credit(100) }

        assertThatThrownBy { useCase.execute(account.id, owner) }.isInstanceOf(BalanceNotZeroException::class.java)
        assertThat(statusOfAccount()).isEqualTo(AccountStatus.ACTIVE)
        assertThat(outbox.events).isEmpty()
    }

    @Test
    fun `closing twice is AccountClosedException, without asking transfers nor a second event`() {
        useCase.execute(account.id, owner)

        assertThatThrownBy { useCase.execute(account.id, owner) }.isInstanceOf(AccountClosedException::class.java)
        assertThat(outbox.events).hasSize(1)
        assertThat(transfers.lookups).hasSize(1)
    }

    @Test
    fun `an account closed while the transfers service is asked is AccountClosedException without a second event`() {
        transfers.duringLookup = { accounts.updateStatus(account.copy(status = AccountStatus.CLOSED)) }

        assertThatThrownBy { useCase.execute(account.id, owner) }.isInstanceOf(AccountClosedException::class.java)
        assertThat(outbox.events).isEmpty()
    }

    @Test
    fun `another customer, or a service, gets AccountNotFoundException and transfers is not asked`() {
        for (caller in listOf(Caller.Customer(UUID.randomUUID()), Caller.Service)) {
            assertThatThrownBy { useCase.execute(account.id, caller) }.isInstanceOf(AccountNotFoundException::class.java)
        }
        assertThat(transfers.lookups).isEmpty()
        assertThat(statusOfAccount()).isEqualTo(AccountStatus.ACTIVE)
    }

    @Test
    fun `an unknown account is AccountNotFoundException`() {
        assertThatThrownBy { useCase.execute(UUID.randomUUID(), owner) }.isInstanceOf(AccountNotFoundException::class.java)
    }
}
