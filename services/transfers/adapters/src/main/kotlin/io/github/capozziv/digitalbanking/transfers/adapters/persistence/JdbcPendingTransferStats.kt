package io.github.capozziv.digitalbanking.transfers.adapters.persistence

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.OffsetDateTime

/** Pending transfers for the metrics: how many, and since when the oldest one waits. */
data class PendingTransfers(
    val count: Long,
    val oldestCreatedAt: Instant?,
)

/**
 * One aggregate query per call (`count` and `min(created_at)` of the pending transfers). The partial index
 * `transfer_due_idx` is on `next_attempt_at`, so the query reads the pending rows from the table; fine while few are
 * pending. Each gauge calls it, so a metrics scrape costs two queries.
 */
@Repository
class JdbcPendingTransferStats(
    private val jdbcClient: JdbcClient,
) {
    fun pending(): PendingTransfers =
        jdbcClient
            .sql("select count(*) as pending, min(created_at) as oldest from transfer where status = 'PENDING'")
            .query { rs, _ ->
                PendingTransfers(rs.getLong("pending"), rs.getObject("oldest", OffsetDateTime::class.java)?.toInstant())
            }.single()
}
