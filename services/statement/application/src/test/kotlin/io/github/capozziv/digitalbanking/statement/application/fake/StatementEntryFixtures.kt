package io.github.capozziv.digitalbanking.statement.application.fake

import io.github.capozziv.digitalbanking.statement.domain.EntryKind
import io.github.capozziv.digitalbanking.statement.domain.StatementEntry
import java.time.Instant
import java.util.UUID

fun statementEntry(
    accountId: UUID,
    sequence: Long,
    entryId: UUID = UUID.randomUUID(),
) = StatementEntry(
    entryId = entryId,
    accountId = accountId,
    sequence = sequence,
    kind = EntryKind.CREDIT,
    amountCents = 1_000,
    balanceAfterCents = 1_000 * sequence,
    description = "deposit $sequence",
    createdAt = Instant.parse("2026-11-10T12:00:00Z").plusSeconds(sequence),
)
