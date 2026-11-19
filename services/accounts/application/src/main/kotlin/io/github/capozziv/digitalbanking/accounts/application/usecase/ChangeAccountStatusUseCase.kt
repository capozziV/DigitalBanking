package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.AccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.port.EventOutbox
import io.github.capozziv.digitalbanking.accounts.application.port.LedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.accounts.domain.AccountStatus
import io.github.capozziv.digitalbanking.accounts.domain.currentBalance
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Blocks or unblocks an account (admin only, checked by the route) under the account lock, so the change is
 * serialized with debits and credits: a debit that takes the lock after the block sees BLOCKED. The same status
 * writes nothing and appends no event; a closed account fails with AccountClosedException.
 */
class ChangeAccountStatusUseCase(
    private val accounts: AccountRepository,
    private val entries: LedgerEntryRepository,
    private val types: AccountTypeRepository,
    private val outbox: EventOutbox,
    private val tx: TransactionRunner,
    private val clock: Clock,
) {
    fun execute(
        accountId: UUID,
        newStatus: AccountStatus,
    ): AccountView =
        tx.inTransaction {
            val account = accounts.lockById(accountId) ?: throw AccountNotFoundException(accountId)
            val current =
                account.changeStatus(newStatus, Instant.now(clock))?.let { (changed, event) ->
                    accounts.updateStatus(changed)
                    outbox.append(event)
                    changed
                } ?: account
            AccountView.of(current, entries.findLast(accountId).currentBalance(), types.findLimit(current.type))
        }
}
