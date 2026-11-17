package io.github.capozziv.digitalbanking.accounts.domain

import java.time.Instant
import java.util.UUID

data class Account(
    val id: UUID,
    val customerId: UUID,
    val type: AccountType,
    val status: AccountStatus = AccountStatus.ACTIVE,
) {
    fun credit(
        last: LedgerEntry?,
        entryId: UUID,
        amount: Money,
        description: String?,
        idempotencyKey: String,
        now: Instant,
    ): EntryRecorded {
        requireOwnEntry(last)
        if (status == AccountStatus.CLOSED) throw AccountClosedException(id)
        return record(last, EntryKind.CREDIT, last.currentBalance() + amount, entryId, amount, description, idempotencyKey, now)
    }

    fun debit(
        last: LedgerEntry?,
        limit: OverdraftLimit,
        entryId: UUID,
        amount: Money,
        description: String?,
        idempotencyKey: String,
        now: Instant,
    ): DebitResult {
        requireOwnEntry(last)
        when (status) {
            AccountStatus.ACTIVE -> Unit
            AccountStatus.BLOCKED -> throw AccountBlockedException(id)
            AccountStatus.CLOSED -> throw AccountClosedException(id)
        }
        val available = AvailableBalance.of(last.currentBalance(), limit)
        if (!available.covers(amount)) return DebitResult.Refused(available)
        return DebitResult.Recorded(
            record(last, EntryKind.DEBIT, last.currentBalance() - amount, entryId, amount, description, idempotencyKey, now),
        )
    }

    fun changeType(
        newType: AccountType,
        newTypeLimit: OverdraftLimit,
    ): Pair<Account, AccountTypeChanged>? {
        if (newType == type) return null
        return copy(type = newType) to AccountTypeChanged(id, type, newType, newTypeLimit)
    }

    /**
     * Blocks or unblocks the account (admin only). Closing goes through [close], never through here. The same status
     * returns null: nothing to write, no event.
     */
    fun changeStatus(
        newStatus: AccountStatus,
        now: Instant,
    ): Pair<Account, AccountEvent>? {
        require(newStatus != AccountStatus.CLOSED) { "an account is closed by close(), not by a status change" }
        if (status == AccountStatus.CLOSED) throw AccountClosedException(id)
        if (newStatus == status) return null
        val event =
            when (newStatus) {
                AccountStatus.BLOCKED -> AccountBlocked(id, customerId, now)
                else -> AccountUnblocked(id, customerId, now)
            }
        return copy(status = newStatus) to event
    }

    /**
     * Closes the account for good. [balance] must be the current balance, read under the account lock; only exactly
     * zero is accepted. A blocked account can be closed; a closed one cannot be closed again.
     */
    fun close(
        balance: Balance,
        now: Instant,
    ): Pair<Account, AccountClosed> {
        if (status == AccountStatus.CLOSED) throw AccountClosedException(id)
        if (balance != Balance.ZERO) throw BalanceNotZeroException(id, balance)
        return copy(status = AccountStatus.CLOSED) to AccountClosed(id, customerId, now)
    }

    private fun requireOwnEntry(last: LedgerEntry?) {
        require(last == null || last.accountId == id) { "last entry belongs to another account" }
    }

    private fun record(
        last: LedgerEntry?,
        kind: EntryKind,
        balanceAfter: Balance,
        entryId: UUID,
        amount: Money,
        description: String?,
        idempotencyKey: String,
        now: Instant,
    ): EntryRecorded {
        val entry =
            LedgerEntry(
                id = entryId,
                accountId = id,
                sequence = last.nextSequence(),
                kind = kind,
                amount = amount,
                balanceAfter = balanceAfter,
                description = description,
                idempotencyKey = idempotencyKey,
                createdAt = now,
            )
        return EntryRecorded(entry, LedgerEntryRecorded(entry))
    }

    companion object {
        fun open(
            id: UUID,
            customerId: UUID,
        ): Pair<Account, AccountOpened> {
            val account = Account(id, customerId, AccountType.FREE)
            return account to AccountOpened(id, customerId, account.type)
        }
    }
}
