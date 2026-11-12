package io.github.capozziv.digitalbanking.statement.application.port

import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import java.util.UUID

/** Cache of the first statement page (default limit, no cursor). Implementations never throw: a failure reads as a miss. */
interface StatementFirstPageCache {
    fun get(accountId: UUID): StatementPage?

    fun put(
        accountId: UUID,
        page: StatementPage,
    )

    fun evict(accountId: UUID)
}
