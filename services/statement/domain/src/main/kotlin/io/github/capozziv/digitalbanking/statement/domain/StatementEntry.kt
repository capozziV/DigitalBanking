package io.github.capozziv.digitalbanking.statement.domain

import java.time.Instant
import java.util.UUID

/**
 * A ledger entry projected into the statement. [sequence] is the entry's position in the account's ledger and,
 * with [accountId], its identity in the statement. [balanceAfterCents] is negative when the account is in overdraft.
 */
data class StatementEntry(
    val entryId: UUID,
    val accountId: UUID,
    val sequence: Long,
    val kind: EntryKind,
    val amountCents: Long,
    val balanceAfterCents: Long,
    val description: String?,
    val createdAt: Instant,
) {
    init {
        require(sequence >= 1) { "sequence must be at least 1, was $sequence" }
        require(amountCents > 0) { "amountCents must be positive, was $amountCents" }
        require(description == null || description.length in 1..MAX_DESCRIPTION_LENGTH) {
            "description must have 1 to $MAX_DESCRIPTION_LENGTH characters"
        }
    }

    companion object {
        const val MAX_DESCRIPTION_LENGTH = 140
    }
}
