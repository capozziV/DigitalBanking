package io.github.capozziv.digitalbanking.statement.application.usecase

import io.github.capozziv.digitalbanking.statement.application.ConflictingStatementEntryException
import io.github.capozziv.digitalbanking.statement.application.port.StatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.application.port.StatementRepository
import io.github.capozziv.digitalbanking.statement.domain.StatementEntry

enum class RecordOutcome { RECORDED, DUPLICATE }

class RecordStatementEntryUseCase(
    private val repository: StatementRepository,
    private val cache: StatementFirstPageCache,
) {
    /**
     * Idempotent by account and sequence: a redelivered entry is a [RecordOutcome.DUPLICATE]. The cached first page
     * is evicted for both outcomes (a duplicate may follow a failed eviction); a conflict throws before any eviction.
     */
    fun execute(entry: StatementEntry): RecordOutcome {
        if (repository.insertEntry(entry)) {
            cache.evict(entry.accountId)
            return RecordOutcome.RECORDED
        }
        val stored =
            checkNotNull(repository.findEntry(entry.accountId, entry.sequence)) {
                "entry ${entry.accountId}/${entry.sequence} was rejected as existing but cannot be read"
            }
        if (stored.entryId != entry.entryId) {
            throw ConflictingStatementEntryException(entry.accountId, entry.sequence, stored.entryId, entry.entryId)
        }
        cache.evict(entry.accountId)
        return RecordOutcome.DUPLICATE
    }
}
