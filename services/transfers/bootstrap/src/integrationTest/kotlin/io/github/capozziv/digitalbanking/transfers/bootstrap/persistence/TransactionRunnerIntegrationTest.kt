package io.github.capozziv.digitalbanking.transfers.bootstrap.persistence

import io.github.capozziv.digitalbanking.transfers.application.port.EventOutbox
import io.github.capozziv.digitalbanking.transfers.application.port.StepRecord
import io.github.capozziv.digitalbanking.transfers.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.application.port.TransferStepLog
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import io.github.capozziv.digitalbanking.transfers.domain.TransferEvent
import io.github.capozziv.digitalbanking.transfers.domain.TransferStep
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.IOException
import java.lang.reflect.UndeclaredThrowableException

/** A failure after the transfer save (step log, outbox) must undo everything the transaction wrote. */
class TransactionRunnerIntegrationTest : TransfersIntegrationTest() {
    @Autowired
    private lateinit var runner: TransactionRunner

    @Autowired
    private lateinit var repository: TransferRepository

    @Autowired
    private lateinit var stepLog: TransferStepLog

    @BeforeEach
    fun cleanTables() {
        ownerJdbc.sql("delete from transfer_step").update()
        ownerJdbc.sql("delete from transfer").update()
    }

    private val failingOutbox =
        object : EventOutbox {
            override fun append(event: TransferEvent): Unit = throw IllegalStateException("outbox down")
        }

    /** A leased transfer and the terminal state the saga would save for it. */
    private fun leased(): Pair<Transfer, Transfer> {
        val transfer = aTransfer()
        repository.insert(transfer)
        val now = transfer.createdAt
        repository.tryAcquireLease(transfer.id, "a", now.plusSeconds(30), now)
        return transfer to transfer.copy(step = TransferStep.Completed, updatedAt = now)
    }

    private fun stepRows(transfer: Transfer): Int =
        ownerJdbc
            .sql("select count(*) from transfer_step where transfer_id = :id")
            .param("id", transfer.id)
            .query(Int::class.java)
            .single()

    private fun saveAndLog(
        transfer: Transfer,
        done: Transfer,
    ) {
        repository.saveIfOwner(done, "a", null, done.updatedAt.plusSeconds(30), done.updatedAt)
        stepLog.append(StepRecord(transfer.id, "CREDIT", "SUCCESS", 201, null, done.updatedAt))
    }

    @Test
    fun `an outbox failure rolls back the transfer save and the step record`() {
        val (transfer, done) = leased()
        val event =
            TransferEvent.TransferCompleted(transfer.id, transfer.sourceAccountId, transfer.destinationAccountId, transfer.amount, null)

        assertThatThrownBy {
            runner.inTransaction {
                saveAndLog(transfer, done)
                failingOutbox.append(event)
            }
        }.isInstanceOf(IllegalStateException::class.java).hasMessage("outbox down")

        assertThat(repository.findById(transfer.id)).isEqualTo(transfer)
        assertThat(stepRows(transfer)).isZero()
    }

    @Test
    fun `a checked exception also rolls back and reaches the caller wrapped`() {
        val (transfer, done) = leased()

        assertThatThrownBy {
            runner.inTransaction {
                saveAndLog(transfer, done)
                throw IOException("disk")
            }
        }.isInstanceOf(UndeclaredThrowableException::class.java).hasCauseInstanceOf(IOException::class.java)

        assertThat(repository.findById(transfer.id)).isEqualTo(transfer)
        assertThat(stepRows(transfer)).isZero()
    }

    @Test
    fun `a block that finishes normally commits and returns its value`() {
        val (transfer, done) = leased()

        val value =
            runner.inTransaction {
                saveAndLog(transfer, done)
                "result"
            }

        assertThat(value).isEqualTo("result")
        assertThat(repository.findById(transfer.id)).isEqualTo(done)
        assertThat(stepRows(transfer)).isEqualTo(1)
    }
}
