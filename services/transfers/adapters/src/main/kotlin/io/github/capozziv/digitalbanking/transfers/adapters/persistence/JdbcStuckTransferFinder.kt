package io.github.capozziv.digitalbanking.transfers.adapters.persistence

import io.github.capozziv.digitalbanking.transfers.application.port.RefusedCompensation
import io.github.capozziv.digitalbanking.transfers.application.port.StuckTransferFinder
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Pending transfers in `COMPENSATE` whose latest step record is a refusal. The latest record of each transfer comes
 * from `transfer_step_transfer_idx` (`transfer_id, id`); the pending rows are read from the table, fine while few are
 * pending (as in [JdbcPendingTransferStats]).
 */
@Repository
class JdbcStuckTransferFinder(
    private val jdbcClient: JdbcClient,
) : StuckTransferFinder {
    override fun findRefusedCompensations(limit: Int): List<RefusedCompensation> =
        jdbcClient
            .sql(
                """
                select t.id, last.detail, last.created_at
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
            .query { rs, _ ->
                RefusedCompensation(
                    transferId = rs.getObject("id", UUID::class.java),
                    reason = rs.getString("detail"),
                    refusedAt = rs.getObject("created_at", OffsetDateTime::class.java).toInstant(),
                )
            }.list()
}
