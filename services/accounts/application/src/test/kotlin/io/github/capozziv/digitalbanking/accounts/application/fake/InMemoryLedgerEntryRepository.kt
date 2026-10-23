package io.github.capozziv.digitalbanking.accounts.application.fake

import io.github.capozziv.digitalbanking.accounts.application.port.LedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
import java.util.UUID

class InMemoryLedgerEntryRepository : LedgerEntryRepository {
    private val entries = mutableListOf<LedgerEntry>()

    override fun findLast(accountId: UUID): LedgerEntry? = entries.filter { it.accountId == accountId }.maxByOrNull { it.sequence }

    override fun findByIdempotencyKey(
        accountId: UUID,
        key: String,
    ): LedgerEntry? = entries.find { it.accountId == accountId && it.idempotencyKey == key }

    override fun insert(entry: LedgerEntry) {
        entries += entry
    }

    fun all(): List<LedgerEntry> = entries.toList()
}
