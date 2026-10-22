package io.github.capozziv.digitalbanking.accounts.application.port

import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
import java.util.UUID

interface LedgerEntryRepository {
    fun findLast(accountId: UUID): LedgerEntry?

    fun findByIdempotencyKey(
        accountId: UUID,
        key: String,
    ): LedgerEntry?

    fun insert(entry: LedgerEntry)
}
