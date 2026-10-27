package io.github.capozziv.digitalbanking.transfers.bootstrap.persistence

import io.github.capozziv.digitalbanking.transfers.application.port.StepRecord
import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.application.port.TransferStepLog
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.simple.JdbcClient
import java.time.OffsetDateTime

class JdbcTransferStepLogIntegrationTest : TransfersIntegrationTest() {
    @Autowired
    private lateinit var stepLog: TransferStepLog

    @Autowired
    private lateinit var repository: TransferRepository

    /** Connected as the application user (transfers_app), unlike [ownerJdbc]. */
    @Autowired
    private lateinit var appJdbc: JdbcClient

    @BeforeEach
    fun cleanTables() {
        ownerJdbc.sql("delete from transfer_step").update()
        ownerJdbc.sql("delete from transfer").update()
    }

    @Test
    fun `append stores every field of the record`() {
        val transfer = aTransfer()
        repository.insert(transfer)
        val at = microsNow()

        stepLog.append(StepRecord(transfer.id, "DEBIT", "REFUSED", 422, "insufficient-available-balance", at))
        stepLog.append(StepRecord(transfer.id, "CREDIT", "TRANSIENT_FAILURE", null, null, at.plusSeconds(1)))

        val rows =
            ownerJdbc
                .sql("select step, outcome, http_status, detail, created_at from transfer_step where transfer_id = :id order by id")
                .param("id", transfer.id)
                .query { rs, _ ->
                    listOf(
                        rs.getString("step"),
                        rs.getString("outcome"),
                        rs.getObject("http_status"),
                        rs.getString("detail"),
                        rs.getObject("created_at", OffsetDateTime::class.java).toInstant(),
                    )
                }.list()
        assertThat(rows).containsExactly(
            listOf("DEBIT", "REFUSED", 422, "insufficient-available-balance", at),
            listOf("CREDIT", "TRANSIENT_FAILURE", null, null, at.plusSeconds(1)),
        )
    }

    @Test
    fun `the application user cannot update or delete the audit trail`() {
        val transfer = aTransfer()
        repository.insert(transfer)
        stepLog.append(StepRecord(transfer.id, "DEBIT", "SUCCESS", 201, null, microsNow()))

        assertThatThrownBy { appJdbc.sql("update transfer_step set detail = 'x'").update() }
            .isInstanceOf(DataAccessException::class.java)
            .rootCause()
            .hasMessageContaining("permission denied")
        assertThatThrownBy { appJdbc.sql("delete from transfer_step").update() }
            .isInstanceOf(DataAccessException::class.java)
            .rootCause()
            .hasMessageContaining("permission denied")
        assertThat(ownerJdbc.sql("select count(*) from transfer_step").query(Long::class.java).single()).isEqualTo(1L)
    }

    @Test
    fun `an unknown outcome is rejected by the database`() {
        val transfer = aTransfer()
        repository.insert(transfer)

        assertThatThrownBy { stepLog.append(StepRecord(transfer.id, "DEBIT", "WHATEVER", null, null, microsNow())) }
            .isInstanceOf(DataAccessException::class.java)
    }
}
