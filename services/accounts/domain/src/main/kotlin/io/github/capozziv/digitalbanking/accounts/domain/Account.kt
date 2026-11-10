package io.github.capozziv.digitalbanking.accounts.domain

import java.time.Instant
import java.util.UUID

data class Account(
    val id: UUID,
    val customerId: UUID,
    val type: AccountType,
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
