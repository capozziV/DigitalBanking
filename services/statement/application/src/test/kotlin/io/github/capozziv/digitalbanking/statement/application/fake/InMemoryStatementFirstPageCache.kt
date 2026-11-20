package io.github.capozziv.digitalbanking.statement.application.fake

import io.github.capozziv.digitalbanking.statement.application.port.FirstPageLookup
import io.github.capozziv.digitalbanking.statement.application.port.StatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import java.util.UUID

/** Same contract as the Redis cache: an eviction bumps the account's version and a put with an older one is dropped. */
class InMemoryStatementFirstPageCache : StatementFirstPageCache {
    private val pages = mutableMapOf<UUID, StatementPage>()
    private val versions = mutableMapOf<UUID, Long>()
    private val evictionCounts = mutableMapOf<UUID, Int>()

    var gets = 0
        private set
    var puts = 0
        private set

    /** When true, every get answers [FirstPageLookup.Unavailable], as the Redis cache does with Redis down. */
    var unavailable = false

    /** Evictions per account id. */
    val evictions: Map<UUID, Int> get() = evictionCounts

    override fun get(accountId: UUID): FirstPageLookup {
        gets++
        if (unavailable) return FirstPageLookup.Unavailable
        return pages[accountId]?.let { FirstPageLookup.Hit(it) } ?: FirstPageLookup.Miss(versions[accountId] ?: 0)
    }

    override fun put(
        accountId: UUID,
        page: StatementPage,
        version: Long,
    ) {
        puts++
        if ((versions[accountId] ?: 0) == version) pages[accountId] = page
    }

    override fun evict(accountId: UUID) {
        evictionCounts.merge(accountId, 1, Int::plus)
        versions.merge(accountId, 1, Long::plus)
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
