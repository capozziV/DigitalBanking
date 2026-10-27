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

/** One aggregate query over the partial index of pending transfers; read on every metrics scrape. */
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
