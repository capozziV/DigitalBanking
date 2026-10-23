package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
import io.github.capozziv.digitalbanking.accounts.domain.Money
import java.util.UUID

data class RecordEntryCommand(
    val accountId: UUID,
    val amount: Money,
    val description: String?,
    val idempotencyKey: String,
)

data class RecordEntryResult(
    val entry: LedgerEntry,
    val created: Boolean,
)
