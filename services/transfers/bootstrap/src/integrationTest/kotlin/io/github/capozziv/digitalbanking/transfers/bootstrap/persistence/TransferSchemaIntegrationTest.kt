package io.github.capozziv.digitalbanking.transfers.bootstrap.persistence

import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.time.OffsetDateTime
import java.util.UUID

/** The CHECK constraints of `transfer` are the last line of defence; each one is exercised with a raw insert. */
class TransferSchemaIntegrationTest : TransfersIntegrationTest() {
    @BeforeEach
    fun cleanTables() {
        ownerJdbc.sql("delete from transfer_step").update()
        ownerJdbc.sql("delete from transfer").update()
    }

    private fun insertRaw(
        amount: Long = 100,
        source: UUID = UUID.randomUUID(),
        destination: UUID = UUID.randomUUID(),
        status: String = "PENDING",
        step: String = "DEBIT",
        nextAttemptAt: OffsetDateTime? = OffsetDateTime.now(),
    ) {
        ownerJdbc
            .sql(
                """
                insert into transfer (id, source_account_id, destination_account_id, amount, idempotency_key, status, step,
                                      deadline_at, next_attempt_at, attempts, created_at, updated_at)
                values (:id, :source, :destination, :amount, :key, :status, :step, now(), :next, 0, now(), now())
                """.trimIndent(),
            ).param("id", UUID.randomUUID())
            .param("source", source)
            .param("destination", destination)
            .param("amount", amount)
            .param("key", "key-${UUID.randomUUID()}")
            .param("status", status)
            .param("step", step)
            .param("next", nextAttemptAt)
            .update()
    }

    private fun assertViolates(
        constraint: String,
        insert: () -> Unit,
    ) {
        assertThatThrownBy(insert)
            .isInstanceOf(DataIntegrityViolationException::class.java)
            .hasMessageContaining("\"$constraint\"")
    }

    @Test
    fun `a valid row is accepted`() {
        insertRaw()
        insertRaw(status = "COMPLETED", step = "COMPLETED", nextAttemptAt = null)

        val rows = ownerJdbc.sql("select count(*) from transfer").query(Int::class.java).single()
        assertThat(rows).isEqualTo(2)
    }

    @Test
    fun `rejects a zero or negative amount`() {
        assertViolates("transfer_amount_check") { insertRaw(amount = 0) }
        assertViolates("transfer_amount_check") { insertRaw(amount = -5) }
    }

    @Test
    fun `rejects the same source and destination`() {
        val account = UUID.randomUUID()
        assertViolates("transfer_check") { insertRaw(source = account, destination = account) }
    }

    @Test
    fun `rejects an unknown status`() {
        assertViolates("transfer_status_check") { insertRaw(status = "DONE") }
    }

    @Test
    fun `rejects an unknown step`() {
        assertViolates("transfer_step_known") { insertRaw(step = "NOPE") }
    }

    @Test
    fun `rejects a pending transfer without a next attempt`() {
        assertViolates("transfer_pending_has_next_attempt") { insertRaw(nextAttemptAt = null) }
    }
}
