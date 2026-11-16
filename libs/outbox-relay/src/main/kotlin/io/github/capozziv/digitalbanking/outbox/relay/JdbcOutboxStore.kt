package io.github.capozziv.digitalbanking.outbox.relay

import io.github.capozziv.digitalbanking.outbox.PendingEvent
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.transaction.support.TransactionOperations
import java.sql.ResultSet
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * [OutboxStore] on PostgreSQL. Leadership is a transaction-scoped advisory lock
 * (`pg_try_advisory_xact_lock`), so it is released by the commit or rollback of [asLeader].
 * [jdbcClient] must use the same data source as the transaction manager behind [transactions].
 */
class JdbcOutboxStore(
    private val jdbcClient: JdbcClient,
    private val transactions: TransactionOperations,
) : OutboxStore {
    override fun <T : Any> asLeader(
        lockKey: Long,
        block: () -> T,
    ): T? =
        transactions.execute {
            val locked =
                jdbcClient
                    .sql("select pg_try_advisory_xact_lock(:lockKey)")
                    .param("lockKey", lockKey)
                    .query(Boolean::class.java)
                    .single()
            if (locked) block() else null
        }

    override fun fetchPending(limit: Int): List<PendingEvent> =
        jdbcClient
            .sql(FETCH_PENDING_SQL)
            .param("limit", limit)
            .query { rs, _ -> rs.toPendingEvent() }
            .list()

    override fun markPublished(
        position: Long,
        at: Instant,
    ) {
        jdbcClient
            .sql("update outbox set published_at = :at where position = :position")
            .param("at", OffsetDateTime.ofInstant(at, ZoneOffset.UTC))
            .param("position", position)
            .update()
    }

    override fun markFailed(
        position: Long,
        error: String,
    ) {
        jdbcClient
            .sql("update outbox set attempts = attempts + 1, last_error = :error where position = :position")
            .param("error", error)
            .param("position", position)
            .update()
    }

    override fun backlog(
        now: Instant,
        stuckAfterAttempts: Int,
    ): OutboxBacklog =
        jdbcClient
            .sql(BACKLOG_SQL)
            .param("stuckAfterAttempts", stuckAfterAttempts)
            .query { rs, _ ->
                val oldest = rs.getObject("oldest", OffsetDateTime::class.java)?.toInstant()
                OutboxBacklog(
                    pending = rs.getLong("pending"),
                    oldestAge = oldest?.let { maxOf(Duration.between(it, now), Duration.ZERO) },
                    stuckAggregates = rs.getLong("stuck"),
                )
            }.single()

    override fun stuckEvents(limit: Int): List<StuckEvent> =
        jdbcClient
            .sql(STUCK_EVENTS_SQL)
            .param("limit", limit)
            .query { rs, _ ->
                StuckEvent(
                    position = rs.getLong("position"),
                    eventId = rs.getObject("event_id", UUID::class.java),
                    aggregateId = rs.getString("aggregate_id"),
                    eventType = rs.getString("event_type"),
                    occurredAt = rs.getObject("occurred_at", OffsetDateTime::class.java).toInstant(),
                    attempts = rs.getInt("attempts"),
                    lastError = rs.getString("last_error"),
                )
            }.list()

    /**
     * Takes the relay lock with `pg_advisory_xact_lock`, which waits (up to [DISCARD_LOCK_TIMEOUT]) for the cycle in
     * progress instead of giving up: the cycle may have fetched the event and would otherwise record a failure on it
     * after the discard, turning it back into a pending event.
     */
    override fun discard(
        eventId: UUID,
        discardedBy: String,
        reason: String,
        lockKey: Long,
    ): DiscardOutcome =
        transactions.execute {
            jdbcClient.sql("set local lock_timeout = '$DISCARD_LOCK_TIMEOUT'").update()
            jdbcClient
                .sql("select pg_advisory_xact_lock(:lockKey)")
                .param("lockKey", lockKey)
                .query()
                .singleRow()
            val row =
                jdbcClient
                    .sql("select position, published_at, attempts, last_error from outbox where event_id = :eventId for update")
                    .param("eventId", eventId)
                    .query()
                    .listOfRows()
                    .singleOrNull()
            when {
                row == null -> {
                    DiscardOutcome.NOT_FOUND
                }

                row["published_at"] != null -> {
                    DiscardOutcome.ALREADY_PUBLISHED
                }

                row["attempts"] == DISCARDED_ATTEMPTS -> {
                    DiscardOutcome.ALREADY_DISCARDED
                }

                (row["attempts"] as Int) <= 0 -> {
                    DiscardOutcome.NOT_FAILED
                }

                else -> {
                    val note = "Discarded manually by $discardedBy: $reason. Last error: ${row["last_error"]}"
                    jdbcClient
                        .sql("update outbox set attempts = :discarded, last_error = :note where position = :position")
                        .param("discarded", DISCARDED_ATTEMPTS)
                        .param("note", note.take(MAX_LAST_ERROR_LENGTH))
                        .param("position", row["position"])
                        .update()
                    DiscardOutcome.DISCARDED
                }
            }
        }

    private fun ResultSet.toPendingEvent() =
        PendingEvent(
            position = getLong("position"),
            eventId = getObject("event_id", UUID::class.java),
            aggregateId = getString("aggregate_id"),
            eventType = getString("event_type"),
            eventVersion = getInt("event_version"),
            payloadJson = getString("payload"),
            correlationId = getString("correlation_id"),
            occurredAt = getObject("occurred_at", OffsetDateTime::class.java).toInstant(),
            attempts = getInt("attempts"),
        )

    companion object {
        /**
         * Pending count, oldest pending event and distinct aggregates with an unpublished event at the stuck threshold;
         * discarded events are not pending.
         */
        const val BACKLOG_SQL =
            "select count(*) as pending, min(occurred_at) as oldest, " +
                "count(distinct aggregate_id) filter (where attempts >= :stuckAfterAttempts) as stuck " +
                "from outbox where published_at is null and attempts >= 0"

        /**
         * `attempts` of an event discarded by hand ([discard]). It keeps `published_at` empty (the event never left)
         * and needs no column of its own, so the services' outbox tables and grants stay as they are. The relay
         * fetches only `attempts >= 0`, and the stuck-aggregate probe looks only at `attempts > 0`.
         */
        const val DISCARDED_ATTEMPTS = -1

        /** Longest a discard waits for the relay cycle in progress (a cycle lasts at most its maximum duration plus one publish). */
        const val DISCARD_LOCK_TIMEOUT = "30s"

        private const val MAX_LAST_ERROR_LENGTH = 2000

        /** Head failed events of the stuck aggregates, oldest first, read through the partial index `outbox_stuck_idx`. */
        const val STUCK_EVENTS_SQL =
            "select position, event_id, aggregate_id, event_type, occurred_at, attempts, last_error " +
                "from outbox where published_at is null and attempts > 0 " +
                "order by position limit :limit"

        /**
         * Pending events in position order, without the discarded ones and those queued behind a failed event of the
         * same aggregate.
         * Both the scan and the anti-join probe read through the partial index `outbox_unpublished_idx`.
         */
        const val FETCH_PENDING_SQL =
            "select o.position, o.event_id, o.aggregate_id, o.event_type, o.event_version, o.payload, " +
                "o.correlation_id, o.occurred_at, o.attempts " +
                "from outbox o " +
                "where o.published_at is null and o.attempts >= 0 " +
                "and not exists (" +
                "select 1 from outbox stuck " +
                "where stuck.published_at is null and stuck.attempts > 0 " +
                "and stuck.aggregate_id = o.aggregate_id and stuck.position < o.position) " +
                "order by o.position limit :limit"
    }
}
