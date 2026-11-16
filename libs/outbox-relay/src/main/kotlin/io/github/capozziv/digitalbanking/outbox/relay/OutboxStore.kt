package io.github.capozziv.digitalbanking.outbox.relay

import io.github.capozziv.digitalbanking.outbox.PendingEvent
import java.time.Duration
import java.time.Instant
import java.util.UUID

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

    /**
     * Up to [limit] events that hold their aggregate back, oldest first: unpublished, not discarded, with at least
     * one failed attempt. Each is the head of its aggregate, since later events of it are not attempted.
     */
    fun stuckEvents(limit: Int): List<StuckEvent>

    /**
     * Sets the failed event [eventId] aside for good, so the later events of its aggregate get published: the relay
     * no longer fetches it and the backlog no longer counts it. The row is kept, with [discardedBy], [reason] and the
     * last error in `last_error`. Waits for the relay lock [lockKey], so it never interleaves with a relay cycle.
     * Discarding an event that is already discarded changes nothing.
     */
    fun discard(
        eventId: UUID,
        discardedBy: String,
        reason: String,
        lockKey: Long,
    ): DiscardOutcome
}

/** An unpublished event with failed attempts, which keeps the later events of its aggregate waiting. */
data class StuckEvent(
    val position: Long,
    val eventId: UUID,
    val aggregateId: String,
    val eventType: String,
    val occurredAt: Instant,
    val attempts: Int,
    val lastError: String?,
)

/** Result of [OutboxStore.discard]. */
enum class DiscardOutcome {
    DISCARDED,
    ALREADY_DISCARDED,
    NOT_FOUND,
    ALREADY_PUBLISHED,

    /** The event was never attempted or never failed: it does not hold its aggregate back. */
    NOT_FAILED,
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
