package io.github.capozziv.digitalbanking.accounts.adapters.web.dto

import io.github.capozziv.digitalbanking.accounts.application.usecase.AccountView
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeLimit
import io.github.capozziv.digitalbanking.accounts.domain.EntryKind
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
import java.time.Instant
import java.util.UUID

/** Amounts in cents. */
data class AccountResponse(
    val id: UUID,
    val customerId: UUID,
    val type: AccountType,
    val balance: Long,
    val overdraftLimit: Long,
    val availableBalance: Long,
) {
    companion object {
        fun from(view: AccountView) =
            AccountResponse(
                id = view.account.id,
                customerId = view.account.customerId,
                type = view.account.type,
                balance = view.balance.cents,
                overdraftLimit = view.overdraftLimit.cents,
                availableBalance = view.availableBalance.cents,
            )
    }
}

/** Amounts in cents. The idempotency key is not echoed back. */
data class LedgerEntryResponse(
    val id: UUID,
    val accountId: UUID,
    val sequence: Long,
    val kind: EntryKind,
    val amount: Long,
    val balanceAfter: Long,
    val description: String?,
    val createdAt: Instant,
) {
    companion object {
        fun from(entry: LedgerEntry) =
            LedgerEntryResponse(
                id = entry.id,
                accountId = entry.accountId,
                sequence = entry.sequence,
                kind = entry.kind,
                amount = entry.amount.cents,
                balanceAfter = entry.balanceAfter.cents,
                description = entry.description,
                createdAt = entry.createdAt,
            )
    }
}

/** Overdraft limit in cents. */
data class AccountTypeLimitResponse(
    val type: AccountType,
    val overdraftLimit: Long,
    val updatedAt: Instant,
) {
    companion object {
        fun from(limit: AccountTypeLimit) = AccountTypeLimitResponse(limit.type, limit.overdraftLimit.cents, limit.updatedAt)
    }
}
