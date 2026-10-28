package io.github.capozziv.digitalbanking.transfers.adapters.persistence

import io.github.capozziv.digitalbanking.transfers.application.port.StepRecord
import io.github.capozziv.digitalbanking.transfers.application.port.TransferStepLog
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** Append-only audit trail; the application user has no UPDATE or DELETE on `transfer_step`. */
@Repository
class JdbcTransferStepLog(
    private val jdbcClient: JdbcClient,
) : TransferStepLog {
    override fun append(record: StepRecord) {
        jdbcClient
            .sql(
                """
                insert into transfer_step (transfer_id, step, outcome, http_status, detail, created_at)
                values (:transferId, :step, :outcome, :httpStatus, :detail, :createdAt)
                """.trimIndent(),
            ).param("transferId", record.transferId)
            .param("step", record.step)
            .param("outcome", record.outcome)
            .param("httpStatus", record.httpStatus)
            .param("detail", record.detail)
            .param("createdAt", OffsetDateTime.ofInstant(record.at, ZoneOffset.UTC))
            .update()
    }
}
