package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.AccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.port.EventOutbox
import io.github.capozziv.digitalbanking.accounts.application.port.IdGenerator
import io.github.capozziv.digitalbanking.accounts.application.port.LedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.accounts.domain.AvailableBalance
import io.github.capozziv.digitalbanking.accounts.domain.DebitResult
import io.github.capozziv.digitalbanking.accounts.domain.EntryKind
import java.time.Clock
import java.time.Instant

sealed interface DebitOutcome {
    data class Recorded(
        val result: RecordEntryResult,
    ) : DebitOutcome

    data class Refused(
        val availableBalance: AvailableBalance,
    ) : DebitOutcome
}

class RecordDebitUseCase(
    private val accounts: AccountRepository,
    private val entries: LedgerEntryRepository,
    private val types: AccountTypeRepository,
    private val outbox: EventOutbox,
    private val tx: TransactionRunner,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    private val lookup = IdempotentEntryLookup(entries)

    fun execute(command: RecordEntryCommand): DebitOutcome =
        tx.inTransaction {
            val account = accounts.lockById(command.accountId) ?: throw AccountNotFoundException(command.accountId)
            lookup.findRepeated(command, EntryKind.DEBIT)?.let {
                return@inTransaction DebitOutcome.Recorded(RecordEntryResult(it, created = false))
            }
            val last = entries.findLast(account.id)
            val limit = types.findLimit(account.type)
            val result =
                account.debit(last, limit, ids.newId(), command.amount, command.description, command.idempotencyKey, Instant.now(clock))
            when (result) {
                is DebitResult.Refused -> {
                    DebitOutcome.Refused(result.availableBalance)
                }

                is DebitResult.Recorded -> {
                    entries.insert(result.recorded.entry)
                    outbox.append(result.recorded.event)
                    DebitOutcome.Recorded(RecordEntryResult(result.recorded.entry, created = true))
                }
            }
        }
}
