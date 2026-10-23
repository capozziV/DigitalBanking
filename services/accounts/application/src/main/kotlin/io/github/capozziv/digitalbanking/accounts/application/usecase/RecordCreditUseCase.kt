package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.EventOutbox
import io.github.capozziv.digitalbanking.accounts.application.port.IdGenerator
import io.github.capozziv.digitalbanking.accounts.application.port.LedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.accounts.domain.EntryKind
import java.time.Clock
import java.time.Instant

class RecordCreditUseCase(
    private val accounts: AccountRepository,
    private val entries: LedgerEntryRepository,
    private val outbox: EventOutbox,
    private val tx: TransactionRunner,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    private val lookup = IdempotentEntryLookup(entries)

    fun execute(command: RecordEntryCommand): RecordEntryResult =
        tx.inTransaction {
            val account = accounts.lockById(command.accountId) ?: throw AccountNotFoundException(command.accountId)
            lookup.findRepeated(command, EntryKind.CREDIT)?.let { return@inTransaction RecordEntryResult(it, created = false) }
            val last = entries.findLast(account.id)
            val (entry, event) =
                account.credit(last, ids.newId(), command.amount, command.description, command.idempotencyKey, Instant.now(clock))
            entries.insert(entry)
            outbox.append(event)
            RecordEntryResult(entry, created = true)
        }
}
