package io.github.capozziv.digitalbanking.statement.application.fake

import io.github.capozziv.digitalbanking.statement.application.port.StatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import java.util.UUID

class InMemoryStatementFirstPageCache : StatementFirstPageCache {
    private val pages = mutableMapOf<UUID, StatementPage>()
    private val evictionCounts = mutableMapOf<UUID, Int>()

    var gets = 0
        private set
    var puts = 0
        private set

    /** Evictions per account id. */
    val evictions: Map<UUID, Int> get() = evictionCounts

    override fun get(accountId: UUID): StatementPage? {
        gets++
        return pages[accountId]
    }

    override fun put(
        accountId: UUID,
        page: StatementPage,
    ) {
        puts++
        pages[accountId] = page
    }

    override fun evict(accountId: UUID) {
        evictionCounts.merge(accountId, 1, Int::plus)
        pages.remove(accountId)
    }

    /** Reads without counting, for assertions. */
    fun peek(accountId: UUID): StatementPage? = pages[accountId]

    /** Stores without counting, for test setup. */
    fun seed(
        accountId: UUID,
        page: StatementPage,
    ) {
        pages[accountId] = page
    }
}
