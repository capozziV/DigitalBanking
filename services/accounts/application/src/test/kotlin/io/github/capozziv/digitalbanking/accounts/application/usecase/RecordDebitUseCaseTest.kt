package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountBusyException
import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.IdempotencyKeyReusedException
import io.github.capozziv.digitalbanking.accounts.application.fake.CallLog
import io.github.capozziv.digitalbanking.accounts.application.fake.DirectTransactionRunner
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryLedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.RecordingEventOutbox
import io.github.capozziv.digitalbanking.accounts.application.fake.SequentialIdGenerator
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeLimit
import io.github.capozziv.digitalbanking.accounts.domain.AvailableBalance
import io.github.capozziv.digitalbanking.accounts.domain.Balance
import io.github.capozziv.digitalbanking.accounts.domain.EntryKind
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntryRecorded
import io.github.capozziv.digitalbanking.accounts.domain.Money
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class RecordDebitUseCaseTest {
    private val now = Instant.parse("2026-10-22T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val account =
        Account(
            UUID.fromString("0199a0b0-0000-7000-8000-0000000000a1"),
            UUID.fromString("0199a0b0-0000-7000-8000-0000000000c1"),
            AccountType.STANDARD,
        )
    private val accounts = InMemoryAccountRepository().apply { insert(account, "open-key-1") }
    private val entries = InMemoryLedgerEntryRepository()
    private val types =
        InMemoryAccountTypeRepository(
            listOf(AccountTypeLimit(AccountType.STANDARD, OverdraftLimit(50000), Instant.parse("2026-10-01T00:00:00Z"))),
        )
    private val outbox = RecordingEventOutbox()
    private val tx = DirectTransactionRunner()
    private val ids = SequentialIdGenerator()
    private val credits = RecordCreditUseCase(accounts, entries, outbox, tx, ids, clock)
    private val useCase = RecordDebitUseCase(accounts, entries, types, outbox, tx, ids, clock)

    // Every scenario starts with a credit of 1000 (entry id 1, sequence 1).
    private val initialCredit =
        LedgerEntry(
            id = SequentialIdGenerator.idAt(1),
            accountId = account.id,
            sequence = 1,
            kind = EntryKind.CREDIT,
            amount = Money(1000),
            balanceAfter = Balance(1000),
            description = null,
            idempotencyKey = "credit-key-1",
            createdAt = now,
        )

    init {
        credits.execute(RecordEntryCommand(account.id, Money(1000), null, "credit-key-1"))
    }

    private fun debit(
        amount: Long,
        key: String,
        accountId: UUID = account.id,
    ) = RecordEntryCommand(accountId, Money(amount), "rent", key)

    private fun debitEntry(
        id: Long,
        sequence: Long,
        amount: Long,
        balanceAfter: Long,
        key: String,
    ) = LedgerEntry(
        id = SequentialIdGenerator.idAt(id),
        accountId = account.id,
        sequence = sequence,
        kind = EntryKind.DEBIT,
        amount = Money(amount),
        balanceAfter = Balance(balanceAfter),
        description = "rent",
        idempotencyKey = key,
        createdAt = now,
    )

    @Test
    fun `debit within the overdraft limit records an entry and appends ledger entry recorded`() {
        val outcome = useCase.execute(debit(1300, "debit-key-1"))

        val expected = debitEntry(id = 2, sequence = 2, amount = 1300, balanceAfter = -300, key = "debit-key-1")
        assertThat(outcome).isEqualTo(DebitOutcome.Recorded(RecordEntryResult(expected, created = true)))
        assertThat(entries.all()).containsExactly(initialCredit, expected)
        assertThat(outbox.events).containsExactly(LedgerEntryRecorded(initialCredit), LedgerEntryRecorded(expected))
    }

    @Test
    fun `debit above the available balance is refused without recording or publishing anything`() {
        val outcome = useCase.execute(debit(51001, "debit-key-1"))

        assertThat(outcome).isEqualTo(DebitOutcome.Refused(AvailableBalance(51000)))
        assertThat(entries.all()).containsExactly(initialCredit)
        assertThat(outbox.events).containsExactly(LedgerEntryRecorded(initialCredit))
    }

    @Test
    fun `retrying a debit with the same key returns the original entry without a new entry or event`() {
        useCase.execute(debit(300, "debit-key-1"))

        val retry = useCase.execute(debit(300, "debit-key-1"))

        val original = debitEntry(id = 2, sequence = 2, amount = 300, balanceAfter = 700, key = "debit-key-1")
        assertThat(retry).isEqualTo(DebitOutcome.Recorded(RecordEntryResult(original, created = false)))
        assertThat(entries.all()).containsExactly(initialCredit, original)
        assertThat(outbox.events).containsExactly(LedgerEntryRecorded(initialCredit), LedgerEntryRecorded(original))
    }

    @Test
    fun `reusing a debit key with another amount fails without recording anything`() {
        useCase.execute(debit(300, "debit-key-1"))

        assertThatThrownBy { useCase.execute(debit(400, "debit-key-1")) }
            .isInstanceOfSatisfying(IdempotencyKeyReusedException::class.java) {
                assertThat(it.key).isEqualTo("debit-key-1")
            }
        val original = debitEntry(id = 2, sequence = 2, amount = 300, balanceAfter = 700, key = "debit-key-1")
        assertThat(entries.all()).containsExactly(initialCredit, original)
        assertThat(outbox.events).containsExactly(LedgerEntryRecorded(initialCredit), LedgerEntryRecorded(original))
    }

    @Test
    fun `reusing a credit key for a debit of the same amount fails without recording anything`() {
        assertThatThrownBy { useCase.execute(debit(1000, "credit-key-1")) }
            .isInstanceOfSatisfying(IdempotencyKeyReusedException::class.java) {
                assertThat(it.key).isEqualTo("credit-key-1")
            }
        assertThat(entries.all()).containsExactly(initialCredit)
        assertThat(outbox.events).containsExactly(LedgerEntryRecorded(initialCredit))
    }

    @Test
    fun `reusing a debit key for a credit of the same amount fails without recording anything`() {
        useCase.execute(debit(300, "debit-key-1"))

        assertThatThrownBy { credits.execute(RecordEntryCommand(account.id, Money(300), "rent", "debit-key-1")) }
            .isInstanceOf(IdempotencyKeyReusedException::class.java)
        val original = debitEntry(id = 2, sequence = 2, amount = 300, balanceAfter = 700, key = "debit-key-1")
        assertThat(entries.all()).containsExactly(initialCredit, original)
        assertThat(outbox.events).containsExactly(LedgerEntryRecorded(initialCredit), LedgerEntryRecorded(original))
    }

    @Test
    fun `debiting an unknown account fails with account not found`() {
        val unknown = UUID.fromString("0199a0b0-0000-7000-8000-0000000000a9")

        assertThatThrownBy { useCase.execute(debit(100, "debit-key-1", accountId = unknown)) }
            .isInstanceOfSatisfying(AccountNotFoundException::class.java) {
                assertThat(it.accountId).isEqualTo(unknown)
            }
        assertThat(entries.all()).containsExactly(initialCredit)
        assertThat(outbox.events).containsExactly(LedgerEntryRecorded(initialCredit))
    }

    @Test
    fun `debiting a busy account propagates account busy without recording anything`() {
        accounts.busyAccounts += account.id

        assertThatThrownBy { useCase.execute(debit(100, "debit-key-1")) }
            .isInstanceOfSatisfying(AccountBusyException::class.java) {
                assertThat(it.accountId).isEqualTo(account.id)
            }
        assertThat(entries.all()).containsExactly(initialCredit)
        assertThat(outbox.events).containsExactly(LedgerEntryRecorded(initialCredit))
    }

    @Test
    fun `debit follows lock, key check, last entry, limit, entry insert and event append in one transaction`() {
        val log = CallLog()
        val tracked =
            RecordDebitUseCase(
                log.accounts(accounts),
                log.entries(entries),
                log.types(types),
                log.outbox(outbox),
                log.transactions(tx),
                ids,
                clock,
            )

        tracked.execute(debit(300, "debit-key-1"))

        assertThat(log.calls).containsExactly(
            "begin",
            "accounts.lockById",
            "entries.findByIdempotencyKey",
            "entries.findLast",
            "types.findLimit",
            "entries.insert",
            "outbox.append",
            "end",
        )
    }

    @Test
    fun `refused debit stops after reading the limit and writes nothing`() {
        val log = CallLog()
        val tracked =
            RecordDebitUseCase(
                log.accounts(accounts),
                log.entries(entries),
                log.types(types),
                log.outbox(outbox),
                log.transactions(tx),
                ids,
                clock,
            )

        tracked.execute(debit(51001, "debit-key-1"))

        assertThat(log.calls).containsExactly(
            "begin",
            "accounts.lockById",
            "entries.findByIdempotencyKey",
            "entries.findLast",
            "types.findLimit",
            "end",
        )
    }
}
