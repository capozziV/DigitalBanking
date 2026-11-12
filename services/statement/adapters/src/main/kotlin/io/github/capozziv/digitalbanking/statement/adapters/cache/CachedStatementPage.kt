package io.github.capozziv.digitalbanking.statement.adapters.cache

import io.github.capozziv.digitalbanking.statement.domain.EntryKind
import io.github.capozziv.digitalbanking.statement.domain.StatementCursor
import io.github.capozziv.digitalbanking.statement.domain.StatementEntry
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import java.time.Instant
import java.util.UUID

/**
 * JSON shape of a cached first page. It is its own format (the domain carries no annotations), versioned by the key
 * prefix: a change that breaks reading old values bumps `v1`.
 */
data class CachedStatementPage(
    val entries: List<CachedStatementEntry>,
    val nextCursor: Long?,
) {
    fun toDomain() =
        StatementPage(
            entries = entries.map { it.toDomain() },
            nextCursor = nextCursor?.let { StatementCursor(it) },
        )

    companion object {
        fun from(page: StatementPage) =
            CachedStatementPage(
                entries = page.entries.map { CachedStatementEntry.from(it) },
                nextCursor = page.nextCursor?.beforeSequence,
            )
    }
}

data class CachedStatementEntry(
    val entryId: String,
    val accountId: String,
    val sequence: Long,
    val kind: String,
    val amountCents: Long,
    val balanceAfterCents: Long,
    val description: String?,
    val createdAt: String,
) {
    fun toDomain() =
        StatementEntry(
            entryId = UUID.fromString(entryId),
            accountId = UUID.fromString(accountId),
            sequence = sequence,
            kind = EntryKind.valueOf(kind),
            amountCents = amountCents,
            balanceAfterCents = balanceAfterCents,
            description = description,
            createdAt = Instant.parse(createdAt),
        )

    companion object {
        fun from(entry: StatementEntry) =
            CachedStatementEntry(
                entryId = entry.entryId.toString(),
                accountId = entry.accountId.toString(),
                sequence = entry.sequence,
                kind = entry.kind.name,
                amountCents = entry.amountCents,
                balanceAfterCents = entry.balanceAfterCents,
                description = entry.description,
                createdAt = entry.createdAt.toString(),
            )
    }
}
