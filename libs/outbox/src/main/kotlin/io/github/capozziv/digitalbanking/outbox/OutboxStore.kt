package io.github.capozziv.digitalbanking.outbox

import java.time.Duration
import java.time.Instant

/** Outbox storage as the relay sees it. */
interface OutboxStore {
    /**
     * Runs [block] in one transaction holding the relay lock [lockKey]. Returns `null` without running
     * [block] when another instance holds the lock. The lock is released when the transaction ends.
     */
    fun <T> asLeader(
        lockKey: Long,
        block: () -> T,
    ): T?

    /** Up to [limit] unpublished events, in position order. */
    fun fetchPending(limit: Int): List<PendingEvent>

    fun markPublished(
        position: Long,
        at: Instant,
    )

    /** Increments the attempts of the event and records [error] as its last error. */
    fun markFailed(
        position: Long,
        error: String,
    )

    /** How many events wait for publication and how old the oldest one is at [now]. */
    fun backlog(now: Instant): OutboxBacklog
}

/** Unpublished events: their count and the age of the oldest (`null` when there is none). */
data class OutboxBacklog(
    val pending: Long,
    val oldestAge: Duration?,
)
