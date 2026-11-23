package io.github.capozziv.digitalbanking.statement.application.port

import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import java.util.UUID

/**
 * Cache of the first statement page (default limit, no cursor). Implementations never throw: a failure reads as
 * [FirstPageLookup.Unavailable] and a failed write or eviction is dropped.
 */
interface StatementFirstPageCache {
    fun get(accountId: UUID): FirstPageLookup

    /**
     * Stores the page only if the account had no [evict] since the [FirstPageLookup.Miss] that gave [version]: a page
     * read before an eviction must never be cached after it.
     */
    fun put(
        accountId: UUID,
        page: StatementPage,
        version: Long,
    )

    fun evict(accountId: UUID)
}

/** Result of reading the cached first page. */
sealed interface FirstPageLookup {
    data class Hit(
        val page: StatementPage,
    ) : FirstPageLookup

    /** No cached page; [version] counts the evictions of the account and guards the later put. */
    data class Miss(
        val version: Long,
    ) : FirstPageLookup

    /** The cache cannot be read now: the page comes from the repository and is not cached. */
    data object Unavailable : FirstPageLookup
}
