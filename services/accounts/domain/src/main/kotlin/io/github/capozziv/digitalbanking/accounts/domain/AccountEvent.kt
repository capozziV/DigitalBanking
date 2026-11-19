package io.github.capozziv.digitalbanking.accounts.domain

import java.time.Instant
import java.util.UUID

sealed interface AccountEvent {
    val accountId: UUID
}

data class AccountOpened(
    override val accountId: UUID,
    val customerId: UUID,
    val type: AccountType,
) : AccountEvent

data class LedgerEntryRecorded(
    val entry: LedgerEntry,
) : AccountEvent {
    override val accountId: UUID get() = entry.accountId
}

data class AccountTypeChanged(
    override val accountId: UUID,
    val previousType: AccountType,
    val newType: AccountType,
    val overdraftLimit: OverdraftLimit,
) : AccountEvent

data class AccountBlocked(
    override val accountId: UUID,
    val customerId: UUID,
    val occurredAt: Instant,
) : AccountEvent

data class AccountUnblocked(
    override val accountId: UUID,
    val customerId: UUID,
    val occurredAt: Instant,
) : AccountEvent

data class AccountClosed(
    override val accountId: UUID,
    val customerId: UUID,
    val occurredAt: Instant,
) : AccountEvent
