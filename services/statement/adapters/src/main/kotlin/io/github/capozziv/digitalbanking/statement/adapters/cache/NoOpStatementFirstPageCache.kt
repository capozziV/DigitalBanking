package io.github.capozziv.digitalbanking.statement.adapters.cache

import io.github.capozziv.digitalbanking.statement.application.port.StatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import org.springframework.stereotype.Component
import java.util.UUID

/** Temporary cache that never holds a page; the Redis adapter replaces it. */
@Component
class NoOpStatementFirstPageCache : StatementFirstPageCache {
    override fun get(accountId: UUID): StatementPage? = null

    override fun put(
        accountId: UUID,
        page: StatementPage,
    ) = Unit

    override fun evict(accountId: UUID) = Unit
}
