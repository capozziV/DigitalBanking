package io.github.capozziv.digitalbanking.transfers.adapters.persistence

import io.github.capozziv.digitalbanking.transfers.application.port.ManualResolution
import io.github.capozziv.digitalbanking.transfers.application.port.ManualResolutionLog
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/** `transfer_manual_resolution`: append-only, one row per transfer (its primary key). */
@Repository
class JdbcManualResolutionLog(
    private val jdbcClient: JdbcClient,
) : ManualResolutionLog {
    override fun append(resolution: ManualResolution) {
        jdbcClient
            .sql(
                """
                insert into transfer_manual_resolution (transfer_id, resolved_by, reason, resolved_at)
                values (:transferId, :resolvedBy, :reason, :resolvedAt)
                """.trimIndent(),
            ).param("transferId", resolution.transferId)
            .param("resolvedBy", resolution.resolvedBy)
            .param("reason", resolution.reason)
            .param("resolvedAt", OffsetDateTime.ofInstant(resolution.resolvedAt, ZoneOffset.UTC))
            .update()
    }

    override fun find(transferId: UUID): ManualResolution? =
        jdbcClient
            .sql("select transfer_id, resolved_by, reason, resolved_at from transfer_manual_resolution where transfer_id = :id")
            .param("id", transferId)
            .query { rs, _ ->
                ManualResolution(
                    transferId = rs.getObject("transfer_id", UUID::class.java),
                    resolvedBy = rs.getObject("resolved_by", UUID::class.java),
                    reason = rs.getString("reason"),
                    resolvedAt = rs.getObject("resolved_at", OffsetDateTime::class.java).toInstant(),
                )
            }.optional()
            .orElse(null)
}
