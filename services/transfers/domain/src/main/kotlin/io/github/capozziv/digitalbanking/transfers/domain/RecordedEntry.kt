package io.github.capozziv.digitalbanking.transfers.domain

import java.util.UUID

/** Whether a ledger entry of `accounts` raised or lowered the balance. */
enum class EntryKind { CREDIT, DEBIT }

/**
 * A ledger entry as `accounts` reports it, read back by its idempotency key: the account it was recorded on, its kind
 * and its amount. A read copy owned by `transfers`, not the ledger entry of `accounts`.
 */
data class RecordedEntry(
    val accountId: UUID,
    val kind: EntryKind,
    val amount: Money,
)
