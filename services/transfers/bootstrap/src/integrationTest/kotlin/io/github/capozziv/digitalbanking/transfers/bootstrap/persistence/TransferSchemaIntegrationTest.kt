package io.github.capozziv.digitalbanking.transfers.bootstrap.persistence

import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
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

    @Test
    fun `a valid row is accepted`() {
        insertRaw()
        insertRaw(status = "COMPLETED", step = "COMPLETED", nextAttemptAt = null)
    }

    @Test
    fun `rejects a zero or negative amount`() {
        assertThatThrownBy { insertRaw(amount = 0) }.isInstanceOf(DataIntegrityViolationException::class.java)
        assertThatThrownBy { insertRaw(amount = -5) }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `rejects the same source and destination`() {
        val account = UUID.randomUUID()
        assertThatThrownBy { insertRaw(source = account, destination = account) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `rejects an unknown status`() {
        assertThatThrownBy { insertRaw(status = "DONE") }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `rejects an unknown step`() {
        assertThatThrownBy { insertRaw(step = "NOPE") }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `rejects a pending transfer without a next attempt`() {
        assertThatThrownBy { insertRaw(nextAttemptAt = null) }.isInstanceOf(DataIntegrityViolationException::class.java)
    }
}
