package io.github.capozziv.digitalbanking.accounts.domain

import java.time.Instant
import java.util.UUID

data class LedgerEntry(
    val id: UUID,
    val accountId: UUID,
    val sequence: Long,
    val kind: EntryKind,
    val amount: Money,
    val balanceAfter: Balance,
    val description: String?,
    val idempotencyKey: String,
    val createdAt: Instant,
) {
    init {
        require(sequence >= 1) { "sequence must be at least 1" }
        require(description == null || description.length in 1..MAX_DESCRIPTION_LENGTH) {
            "description must have 1 to $MAX_DESCRIPTION_LENGTH characters"
        }
    }

    companion object {
        const val MAX_DESCRIPTION_LENGTH = 140
    }
}

fun LedgerEntry?.currentBalance(): Balance = this?.balanceAfter ?: Balance.ZERO

fun LedgerEntry?.nextSequence(): Long = if (this == null) 1 else sequence + 1
