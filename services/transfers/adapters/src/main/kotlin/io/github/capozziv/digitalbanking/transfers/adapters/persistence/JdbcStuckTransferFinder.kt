package io.github.capozziv.digitalbanking.transfers.adapters.persistence

import io.github.capozziv.digitalbanking.transfers.application.port.StuckTransfer
import io.github.capozziv.digitalbanking.transfers.application.port.StuckTransferFinder
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

/**
 * Pending transfers in `COMPENSATE` whose latest step record is a refusal, read with their transfer row in one query.
 * The latest record of each transfer comes from `transfer_step_transfer_idx` (`transfer_id, id`); the pending rows are
 * read from the table, fine while few are pending (as in [JdbcPendingTransferStats]).
 */
@Repository
class JdbcStuckTransferFinder(
    private val jdbcClient: JdbcClient,
) : StuckTransferFinder {
    override fun findStuckCompensations(limit: Int): List<StuckTransfer> =
        jdbcClient
            .sql(
                """
                select $TRANSFER_COLUMNS, last.detail as refusal, last.created_at as refused_at
                from transfer t
                join lateral (
                    select s.outcome, s.detail, s.created_at
                    from transfer_step s
                    where s.transfer_id = t.id
                    order by s.id desc
                    limit 1
                ) last on true
                where t.status = 'PENDING' and t.step = 'COMPENSATE' and last.outcome = 'REFUSED'
                order by t.created_at, t.id
                limit :limit
                """.trimIndent(),
            ).param("limit", limit)
            .query { rs, rowNum ->
                StuckTransfer(
                    transfer = TransferRows.ROW_MAPPER.mapRow(rs, rowNum),
                    refusal = rs.getString("refusal"),
                    refusedAt = rs.getObject("refused_at", OffsetDateTime::class.java).toInstant(),
                )
            }.list()

    private companion object {
        val TRANSFER_COLUMNS = TransferRows.COLUMNS.split(", ").joinToString(", ") { "t.$it" }
    }
}
