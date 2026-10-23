package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountBusyException
import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.IdempotencyKeyReusedException
import io.github.capozziv.digitalbanking.accounts.application.fake.CallLog
import io.github.capozziv.digitalbanking.accounts.application.fake.DirectTransactionRunner
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryLedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.RecordingEventOutbox
import io.github.capozziv.digitalbanking.accounts.application.fake.SequentialIdGenerator
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.Balance
import io.github.capozziv.digitalbanking.accounts.domain.EntryKind
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntryRecorded
import io.github.capozziv.digitalbanking.accounts.domain.Money
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class RecordCreditUseCaseTest {
    private val now = Instant.parse("2026-10-22T12:00:00Z")
    private val account =
        Account(
            UUID.fromString("0199a0b0-0000-7000-8000-0000000000a1"),
            UUID.fromString("0199a0b0-0000-7000-8000-0000000000c1"),
            AccountType.FREE,
        )
    private val accounts = InMemoryAccountRepository().apply { insert(account, "open-key-1") }
    private val entries = InMemoryLedgerEntryRepository()
    private val outbox = RecordingEventOutbox()
    private val tx = DirectTransactionRunner()
    private val useCase =
        RecordCreditUseCase(accounts, entries, outbox, tx, SequentialIdGenerator(), Clock.fixed(now, ZoneOffset.UTC))

    private val firstCredit =
        LedgerEntry(
            id = SequentialIdGenerator.idAt(1),
            accountId = account.id,
            sequence = 1,
            kind = EntryKind.CREDIT,
            amount = Money(1500),
            balanceAfter = Balance(1500),
            description = "salary",
            idempotencyKey = "credit-key-1",
            createdAt = now,
        )

    private fun credit(
        amount: Long,
        key: String,
        description: String? = "salary",
        accountId: UUID = account.id,
    ) = RecordEntryCommand(accountId, Money(amount), description, key)

    @Test
    fun `credit records an entry and appends ledger entry recorded to the outbox`() {
        val result = useCase.execute(credit(1500, "credit-key-1"))

        assertThat(result).isEqualTo(RecordEntryResult(firstCredit, created = true))
        assertThat(entries.all()).containsExactly(firstCredit)
        assertThat(outbox.events).containsExactly(LedgerEntryRecorded(firstCredit))
        assertThat(tx.transactions).isEqualTo(1)
    }

    @Test
    fun `credit continues the sequence and balance of the last entry`() {
        useCase.execute(credit(1500, "credit-key-1"))

        val result = useCase.execute(credit(700, "credit-key-2", description = null))

        val second =
            LedgerEntry(
                id = SequentialIdGenerator.idAt(2),
                accountId = account.id,
                sequence = 2,
                kind = EntryKind.CREDIT,
                amount = Money(700),
                balanceAfter = Balance(2200),
                description = null,
                idempotencyKey = "credit-key-2",
                createdAt = now,
            )
        assertThat(result).isEqualTo(RecordEntryResult(second, created = true))
        assertThat(entries.all()).containsExactly(firstCredit, second)
        assertThat(outbox.events).containsExactly(LedgerEntryRecorded(firstCredit), LedgerEntryRecorded(second))
    }

    @Test
    fun `retrying a credit with the same key returns the original entry without a new entry or event`() {
        useCase.execute(credit(1500, "credit-key-1"))

        val retry = useCase.execute(credit(1500, "credit-key-1"))

        assertThat(retry).isEqualTo(RecordEntryResult(firstCredit, created = false))
        assertThat(entries.all()).containsExactly(firstCredit)
        assertThat(outbox.events).containsExactly(LedgerEntryRecorded(firstCredit))
    }

    @Test
    fun `reusing a credit key with another amount fails without recording anything`() {
        useCase.execute(credit(1500, "credit-key-1"))

        assertThatThrownBy { useCase.execute(credit(1600, "credit-key-1")) }
            .isInstanceOfSatisfying(IdempotencyKeyReusedException::class.java) {
                assertThat(it.key).isEqualTo("credit-key-1")
            }
        assertThat(entries.all()).containsExactly(firstCredit)
        assertThat(outbox.events).containsExactly(LedgerEntryRecorded(firstCredit))
    }

    @Test
    fun `crediting an unknown account fails with account not found`() {
        val unknown = UUID.fromString("0199a0b0-0000-7000-8000-0000000000a9")

        assertThatThrownBy { useCase.execute(credit(1500, "credit-key-1", accountId = unknown)) }
            .isInstanceOfSatisfying(AccountNotFoundException::class.java) {
                assertThat(it.accountId).isEqualTo(unknown)
            }
        assertThat(entries.all()).isEmpty()
        assertThat(outbox.events).isEmpty()
    }

    @Test
    fun `crediting a busy account propagates account busy without recording anything`() {
        accounts.busyAccounts += account.id

        assertThatThrownBy { useCase.execute(credit(1500, "credit-key-1")) }
            .isInstanceOfSatisfying(AccountBusyException::class.java) {
                assertThat(it.accountId).isEqualTo(account.id)
            }
        assertThat(entries.all()).isEmpty()
        assertThat(outbox.events).isEmpty()
    }

    @Test
    fun `credit follows lock, key check, last entry, entry insert and event append in one transaction`() {
        val log = CallLog()
        val tracked =
            RecordCreditUseCase(
                log.accounts(accounts),
                log.entries(entries),
                log.outbox(outbox),
                log.transactions(tx),
                SequentialIdGenerator(),
                Clock.fixed(now, ZoneOffset.UTC),
            )

        tracked.execute(credit(1500, "credit-key-1"))

        assertThat(log.calls).containsExactly(
            "begin",
            "accounts.lockById",
            "entries.findByIdempotencyKey",
            "entries.findLast",
            "entries.insert",
            "outbox.append",
            "end",
        )
    }
}
