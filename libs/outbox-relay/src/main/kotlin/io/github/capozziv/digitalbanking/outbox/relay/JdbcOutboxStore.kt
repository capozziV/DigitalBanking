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

    override fun backlog(now: Instant): OutboxBacklog =
        jdbcClient
            .sql("select count(*) as pending, min(occurred_at) as oldest from outbox where published_at is null")
            .query { rs, _ ->
                val oldest = rs.getObject("oldest", OffsetDateTime::class.java)?.toInstant()
                OutboxBacklog(
                    pending = rs.getLong("pending"),
                    oldestAge = oldest?.let { maxOf(Duration.between(it, now), Duration.ZERO) },
                )
            }.single()

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
         * Pending events in position order, without those queued behind a failed event of the same aggregate.
         * Both the scan and the anti-join probe read through the partial index `outbox_unpublished_idx`.
         */
        const val FETCH_PENDING_SQL =
            "select o.position, o.event_id, o.aggregate_id, o.event_type, o.event_version, o.payload, " +
                "o.correlation_id, o.occurred_at, o.attempts " +
                "from outbox o " +
                "where o.published_at is null " +
                "and not exists (" +
                "select 1 from outbox stuck " +
                "where stuck.published_at is null and stuck.attempts > 0 " +
                "and stuck.aggregate_id = o.aggregate_id and stuck.position < o.position) " +
                "order by o.position limit :limit"
    }
}
