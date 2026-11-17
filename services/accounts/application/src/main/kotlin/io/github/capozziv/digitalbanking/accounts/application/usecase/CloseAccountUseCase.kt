package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.PendingTransfersException
import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.AccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.port.EventOutbox
import io.github.capozziv.digitalbanking.accounts.application.port.LedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.accounts.application.port.TransfersGateway
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.currentBalance
import io.github.capozziv.digitalbanking.security.Caller
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Closes an account for good, by its owner or an admin.
 *
 * 1. Read-only pre-check: the caller may close it (anyone else gets [AccountNotFoundException], as on reads), it is not
 *    closed and its balance is zero. These refusals never ask the `transfers` service. Keeps the sequence of the last
 *    ledger entry.
 * 2. Asks the `transfers` service, outside any transaction (no connection held), for PENDING transfers leaving the
 *    account: any is [PendingTransfersException]; the service failing is
 *    [io.github.capozziv.digitalbanking.accounts.application.TransfersUnavailableException]. Nothing is written.
 * 3. Locks the account and re-reads status, balance and last sequence under the lock. Any entry recorded since step 1
 *    refuses the closing with [PendingTransfersException], even when the balance is zero again: a debit there may
 *    belong to a transfer the lookup never saw, still PENDING, whose compensation would credit a closed account.
 *    Otherwise writes CLOSED with the AccountClosed event in the same transaction. A debit that takes the lock
 *    afterwards sees CLOSED and is refused, so a transfer created after this step never takes money out.
 */
class CloseAccountUseCase(
    private val accounts: AccountRepository,
    private val entries: LedgerEntryRepository,
    private val types: AccountTypeRepository,
    private val outbox: EventOutbox,
    private val tx: TransactionRunner,
    private val transfers: TransfersGateway,
    private val clock: Clock,
) {
    fun execute(
        accountId: UUID,
        caller: Caller,
    ): AccountView {
        val checkedSequence =
            tx.inReadOnlyTransaction {
                val account = accounts.findById(accountId) ?: throw AccountNotFoundException(accountId)
                caller.requireCanClose(account)
                val last = entries.findLast(accountId)
                account.close(last.currentBalance(), Instant.now(clock))
                last?.sequence
            }
        val pending = transfers.pendingOutgoing(accountId)
        if (pending > 0) throw PendingTransfersException(accountId, pending)
        return tx.inTransaction {
            val account = accounts.lockById(accountId) ?: throw AccountNotFoundException(accountId)
            val last = entries.findLast(accountId)
            val balance = last.currentBalance()
            val (closed, event) = account.close(balance, Instant.now(clock))
            // At least one entry landed after the lookup; its transfer may be the pending one.
            if (last?.sequence != checkedSequence) throw PendingTransfersException(accountId, 1)
            accounts.updateStatus(closed)
            outbox.append(event)
            AccountView.of(closed, balance, types.findLimit(closed.type))
        }
    }

    /** The owner or an admin; anyone else (another customer, a service) is told the account does not exist. */
    private fun Caller.requireCanClose(account: Account) {
        val allowed = this is Caller.Admin || actsAsCustomer(account.customerId)
        if (!allowed) throw AccountNotFoundException(account.id)
    }
}
