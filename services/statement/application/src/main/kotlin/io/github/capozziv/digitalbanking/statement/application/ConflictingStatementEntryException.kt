package io.github.capozziv.digitalbanking.statement.application

import java.util.UUID

/** Another entry already holds this account's sequence: the ledger and the statement disagree. */
class ConflictingStatementEntryException(
    val accountId: UUID,
    val sequence: Long,
    val storedEntryId: UUID,
    val receivedEntryId: UUID,
) : RuntimeException(
        "account $accountId sequence $sequence holds entry $storedEntryId, received entry $receivedEntryId",
    )
