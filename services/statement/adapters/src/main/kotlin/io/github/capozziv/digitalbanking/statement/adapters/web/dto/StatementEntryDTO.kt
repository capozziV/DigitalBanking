package io.github.capozziv.digitalbanking.statement.adapters.web.dto

import io.github.capozziv.digitalbanking.statement.domain.EntryKind
import io.github.capozziv.digitalbanking.statement.domain.StatementEntry
import java.time.Instant
import java.util.UUID

/** One line of the statement. `amount` and `balanceAfter` are in cents; `balanceAfter` is negative in overdraft. */
data class StatementEntryDTO(
    val entryId: UUID,
    val sequence: Long,
    val kind: EntryKind,
    val amount: Long,
    val balanceAfter: Long,
    val description: String?,
    val createdAt: Instant,
) {
    companion object {
        fun from(entry: StatementEntry) =
            StatementEntryDTO(
                entryId = entry.entryId,
                sequence = entry.sequence,
                kind = entry.kind,
                amount = entry.amountCents,
                balanceAfter = entry.balanceAfterCents,
                description = entry.description,
                createdAt = entry.createdAt,
            )
    }
}
