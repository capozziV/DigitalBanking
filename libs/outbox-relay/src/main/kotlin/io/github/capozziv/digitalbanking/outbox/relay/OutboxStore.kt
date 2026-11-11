package io.github.capozziv.digitalbanking.outbox.relay

import io.github.capozziv.digitalbanking.outbox.PendingEvent
import java.time.Duration
import java.time.Instant

/** Outbox storage as the relay sees it. */
interface OutboxStore {
    /**
     * Runs [block] in one transaction holding the relay lock [lockKey]. Returns `null` without running
     * [block] when another instance holds the lock. The lock is released when the transaction ends.
     */
    fun <T : Any> asLeader(
        lockKey: Long,
        block: () -> T,
    ): T?

    /**
     * Up to [limit] unpublished events, in position order, leaving out every event that has an earlier
     * unpublished event of the same aggregate with a failed attempt. A stuck aggregate thus offers only its
     * head event and cannot fill the batch.
     */
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

    /**
     * How many events wait for publication, how old the oldest one is at [now] and how many aggregates are
     * stuck: have an unpublished event with at least [stuckAfterAttempts] failed attempts.
     */
    fun backlog(
        now: Instant,
        stuckAfterAttempts: Int,
    ): OutboxBacklog
}

/**
 * Unpublished events: their count, the age of the oldest (`null` when there is none) and how many aggregates
 * are stuck, that is, have an unpublished event that reached the stuck threshold of failed attempts. Derived
 * from the persisted rows, so it stays right after a restart or a leader change.
 */
data class OutboxBacklog(
    val pending: Long,
    val oldestAge: Duration?,
    val stuckAggregates: Long,
)
