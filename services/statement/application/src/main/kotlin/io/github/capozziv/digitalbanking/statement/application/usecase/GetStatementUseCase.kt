package io.github.capozziv.digitalbanking.statement.application.usecase

import io.github.capozziv.digitalbanking.security.Caller
import io.github.capozziv.digitalbanking.statement.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.statement.application.port.FirstPageLookup
import io.github.capozziv.digitalbanking.statement.application.port.StatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.application.port.StatementRepository
import io.github.capozziv.digitalbanking.statement.domain.StatementCursor
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import java.util.UUID

class GetStatementUseCase(
    private val repository: StatementRepository,
    private val cache: StatementFirstPageCache,
) {
    fun execute(
        accountId: UUID,
        cursor: StatementCursor?,
        limit: Int,
        caller: Caller,
    ): StatementPage {
        require(limit in MIN_LIMIT..MAX_LIMIT) { "limit must be between $MIN_LIMIT and $MAX_LIMIT, was $limit" }
        // The owner check comes before the cache: a cached page must never be served to another customer.
        requireCanRead(accountId, caller)
        val cacheable = cursor == null && limit == DEFAULT_LIMIT
        val lookup = if (cacheable) cache.get(accountId) else FirstPageLookup.Unavailable
        if (lookup is FirstPageLookup.Hit) return lookup.page
        val page = repository.findPage(accountId, cursor, limit)
        // Only an empty first page needs the account item: entries prove the account exists even before
        // its AccountOpened arrives, and a cursor means an earlier page was already served.
        if (cursor == null && page.entries.isEmpty() && !repository.accountExists(accountId)) {
            throw AccountNotFoundException(accountId)
        }
        // The version read with the miss keeps this page out of the cache if an entry evicted it meanwhile (#226).
        if (lookup is FirstPageLookup.Miss) cache.put(accountId, page, lookup.version)
        return page
    }

    /**
     * An admin reads any statement. Anyone else must own the account; a foreign account, an unknown account and an
     * account whose `AccountOpened` has not arrived yet (owner unknown) all answer not found, so the response does
     * not tell whether the account exists.
     */
    private fun requireCanRead(
        accountId: UUID,
        caller: Caller,
    ) {
        if (caller is Caller.Admin) return
        val owner = repository.findCustomerId(accountId)
        if (owner == null || !caller.actsAsCustomer(owner)) throw AccountNotFoundException(accountId)
    }

    companion object {
        const val MIN_LIMIT = 1
        const val MAX_LIMIT = 100
        const val DEFAULT_LIMIT = 20
    }
}
