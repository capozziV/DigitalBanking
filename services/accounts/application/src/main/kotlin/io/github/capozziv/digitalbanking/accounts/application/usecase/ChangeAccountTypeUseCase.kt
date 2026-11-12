package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.AccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.port.EventOutbox
import io.github.capozziv.digitalbanking.accounts.application.port.LedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.currentBalance
import java.util.UUID

/**
 * Changes the account type under the account lock; the event carries the new type's overdraft limit, read in
 * the same transaction. The same type returns the account unchanged, without
 * update or event. A downgrade is accepted even when the balance is below the new overdraft limit: the
 * balance does not take part in the change, it is read only for the returned view, still under the lock.
 */
class ChangeAccountTypeUseCase(
    private val accounts: AccountRepository,
    private val entries: LedgerEntryRepository,
    private val types: AccountTypeRepository,
    private val outbox: EventOutbox,
    private val tx: TransactionRunner,
) {
    fun execute(
        accountId: UUID,
        newType: AccountType,
    ): AccountView =
        tx.inTransaction {
            val account = accounts.lockById(accountId) ?: throw AccountNotFoundException(accountId)
            val newLimit = types.findLimit(newType)
            val current =
                account.changeType(newType, newLimit)?.let { (changed, event) ->
                    accounts.updateType(changed)
                    outbox.append(event)
                    changed
                } ?: account
            AccountView.of(current, entries.findLast(accountId).currentBalance(), newLimit)
        }
}
