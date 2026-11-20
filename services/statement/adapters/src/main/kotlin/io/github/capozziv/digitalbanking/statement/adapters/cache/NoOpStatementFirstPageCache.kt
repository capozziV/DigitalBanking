package io.github.capozziv.digitalbanking.statement.adapters.cache

import io.github.capozziv.digitalbanking.statement.application.port.FirstPageLookup
import io.github.capozziv.digitalbanking.statement.application.port.StatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import java.util.UUID

/** Cache that never holds a page; selected with `statement.cache.enabled=false` (CacheConfiguration). */
class NoOpStatementFirstPageCache : StatementFirstPageCache {
    override fun get(accountId: UUID): FirstPageLookup = FirstPageLookup.Unavailable

    override fun put(
        accountId: UUID,
        page: StatementPage,
        version: Long,
    ) = Unit

    override fun evict(accountId: UUID) = Unit
}
