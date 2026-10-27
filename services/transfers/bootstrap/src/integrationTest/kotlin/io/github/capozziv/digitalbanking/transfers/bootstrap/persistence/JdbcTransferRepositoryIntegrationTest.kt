package io.github.capozziv.digitalbanking.transfers.bootstrap.persistence

import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import io.github.capozziv.digitalbanking.transfers.domain.TransferStep
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.Future

class JdbcTransferRepositoryIntegrationTest : TransfersIntegrationTest() {
    @Autowired
    private lateinit var repository: TransferRepository

    @BeforeEach
    fun cleanTables() {
        ownerJdbc.sql("delete from transfer_step").update()
        ownerJdbc.sql("delete from transfer").update()
    }

    private val allSteps =
        listOf(
            TransferStep.Debit,
            TransferStep.ResolveDebit,
            TransferStep.Credit,
            TransferStep.ResolveCredit,
            TransferStep.Compensate,
            TransferStep.Completed,
            TransferStep.Failed,
            TransferStep.Compensated,
        )

    @Test
    fun `round trips every field of a transfer in each of the 8 steps`() {
        allSteps.forEach { step ->
            val transfer = aTransfer(step = step, failureReason = "some-reason", attempts = 3)
            assertThat(repository.insert(transfer)).isTrue()

            assertThat(repository.findById(transfer.id)).isEqualTo(transfer)
            assertThat(repository.findByIdempotencyKey(transfer.idempotencyKey)).isEqualTo(transfer)
        }
    }

    @Test
    fun `round trips a transfer with null description, failure reason and correlation id`() {
        val transfer = aTransfer().copy(description = null, correlationId = null)
        repository.insert(transfer)

        assertThat(repository.findById(transfer.id)).isEqualTo(transfer)
    }

    @Test
    fun `finds nothing for unknown id and key`() {
        assertThat(repository.findById(UUID.randomUUID())).isNull()
        assertThat(repository.findByIdempotencyKey("nope")).isNull()
    }

    @Test
    fun `insert returns false for a repeated idempotency key and keeps the first row`() {
        val first = aTransfer(idempotencyKey = "same-key")
        val second = aTransfer(idempotencyKey = "same-key")

        assertThat(repository.insert(first)).isTrue()
        assertThat(repository.insert(second)).isFalse()

        assertThat(repository.findByIdempotencyKey("same-key")).isEqualTo(first)
        assertThat(repository.findById(second.id)).isNull()
    }

    @Test
    fun `insert schedules the first attempt at the creation instant`() {
        val transfer = aTransfer()
        repository.insert(transfer)

        assertThat(nextAttemptAt(transfer)).isEqualTo(transfer.createdAt)
        assertThat(repository.findDue(transfer.createdAt, 10)).containsExactly(transfer.id)
    }

    @Test
    fun `two concurrent lease attempts yield exactly one winner`() {
        repeat(10) {
            val transfer = aTransfer()
            repository.insert(transfer)
            val now = microsNow()
            val barrier = CyclicBarrier(2)
            val pool = Executors.newFixedThreadPool(2)
            try {
                val results: List<Future<Boolean>> =
                    listOf("a", "b").map { owner ->
                        pool.submit<Boolean> {
                            barrier.await()
                            repository.tryAcquireLease(transfer.id, owner, now.plusSeconds(30), now)
                        }
                    }
                assertThat(results.map { it.get() }).containsExactlyInAnyOrder(true, false)
            } finally {
                pool.shutdownNow()
            }
        }
    }

    @Test
    fun `an expired lease can be taken by another owner and a valid one cannot`() {
        val transfer = aTransfer()
        repository.insert(transfer)
        val now = microsNow()

        assertThat(repository.tryAcquireLease(transfer.id, "a", now.plusSeconds(30), now)).isTrue()
        assertThat(repository.tryAcquireLease(transfer.id, "b", now.plusSeconds(60), now.plusSeconds(10))).isFalse()
        assertThat(repository.tryAcquireLease(transfer.id, "b", now.plusSeconds(60), now.plusSeconds(31))).isTrue()
        assertThat(lockOwner(transfer)).isEqualTo("b")
    }

    @Test
    fun `the owner can take its own lease again`() {
        val transfer = aTransfer()
        repository.insert(transfer)
        val now = microsNow()

        assertThat(repository.tryAcquireLease(transfer.id, "a", now.plusSeconds(30), now)).isTrue()
        assertThat(repository.tryAcquireLease(transfer.id, "a", now.plusSeconds(60), now.plusSeconds(1))).isTrue()
    }

    @Test
    fun `a terminal transfer cannot be leased`() {
        val transfer = aTransfer(step = TransferStep.Completed)
        repository.insert(transfer)
        val now = microsNow()

        assertThat(repository.tryAcquireLease(transfer.id, "a", now.plusSeconds(30), now)).isFalse()
    }

    @Test
    fun `saveIfOwner by the owner writes state, next attempt and renews the lease`() {
        val transfer = aTransfer()
        repository.insert(transfer)
        val now = transfer.createdAt.plusSeconds(1)
        repository.tryAcquireLease(transfer.id, "a", now.plusSeconds(30), now)
        val next =
            transfer.copy(
                step = TransferStep.Credit,
                attempts = 2,
                updatedAt = now,
            )
        val nextAttempt = now.plusSeconds(5)
        val renewed = now.plusSeconds(60)

        assertThat(repository.saveIfOwner(next, "a", nextAttempt, renewed)).isTrue()

        assertThat(repository.findById(transfer.id)).isEqualTo(next)
        assertThat(nextAttemptAt(transfer)).isEqualTo(nextAttempt)
        assertThat(lockedUntil(transfer)).isEqualTo(renewed)
    }

    @Test
    fun `saveIfOwner by another owner returns false and changes nothing`() {
        val transfer = aTransfer()
        repository.insert(transfer)
        val now = transfer.createdAt.plusSeconds(1)
        repository.tryAcquireLease(transfer.id, "a", now.plusSeconds(30), now)

        val saved = repository.saveIfOwner(transfer.copy(step = TransferStep.Credit, updatedAt = now), "b", now, now.plusSeconds(60))

        assertThat(saved).isFalse()
        assertThat(repository.findById(transfer.id)).isEqualTo(transfer)
        assertThat(nextAttemptAt(transfer)).isEqualTo(transfer.createdAt)
        assertThat(lockedUntil(transfer)).isEqualTo(now.plusSeconds(30))
    }

    @Test
    fun `saveIfOwner after the lease expired returns false even for the former owner`() {
        val transfer = aTransfer()
        repository.insert(transfer)
        val now = transfer.createdAt
        repository.tryAcquireLease(transfer.id, "a", now.plusSeconds(30), now)
        val late = now.plusSeconds(31)

        val saved = repository.saveIfOwner(transfer.copy(step = TransferStep.Credit, updatedAt = late), "a", late, late.plusSeconds(30))

        assertThat(saved).isFalse()
        assertThat(repository.findById(transfer.id)).isEqualTo(transfer)
    }

    @Test
    fun `saveIfOwner without a lease returns false`() {
        val transfer = aTransfer()
        repository.insert(transfer)

        assertThat(repository.saveIfOwner(transfer, "a", transfer.createdAt, transfer.createdAt.plusSeconds(30))).isFalse()
    }

    @Test
    fun `a terminal save stores a null next attempt and the transfer is never due`() {
        val transfer = aTransfer()
        repository.insert(transfer)
        val now = transfer.createdAt
        repository.tryAcquireLease(transfer.id, "a", now.plusSeconds(1), now)
        val done = transfer.copy(step = TransferStep.Completed, updatedAt = now)

        assertThat(repository.saveIfOwner(done, "a", null, now.plusSeconds(1))).isTrue()

        assertThat(nextAttemptAt(transfer)).isNull()
        assertThat(repository.findDue(now.plusSeconds(3600), 10)).isEmpty()
    }

    @Test
    fun `releaseLease frees the lease only for its owner`() {
        val transfer = aTransfer()
        repository.insert(transfer)
        val now = microsNow()
        repository.tryAcquireLease(transfer.id, "a", now.plusSeconds(30), now)

        repository.releaseLease(transfer.id, "b")
        assertThat(repository.tryAcquireLease(transfer.id, "b", now.plusSeconds(30), now)).isFalse()

        repository.releaseLease(transfer.id, "a")
        assertThat(repository.tryAcquireLease(transfer.id, "b", now.plusSeconds(30), now)).isTrue()
    }

    @Test
    fun `findDue skips future, terminal, leased and null-next-attempt transfers and honours the limit`() {
        val now = microsNow()
        val due1 = aTransfer(createdAt = now.minusSeconds(30))
        val due2 = aTransfer(createdAt = now.minusSeconds(20))
        val future = aTransfer(createdAt = now.plusSeconds(60))
        val terminal = aTransfer(createdAt = now.minusSeconds(30), step = TransferStep.Completed)
        val leased = aTransfer(createdAt = now.minusSeconds(30))
        val nullNext = aTransfer(createdAt = now.minusSeconds(30))
        listOf(due1, due2, future, terminal, leased, nullNext).forEach(repository::insert)
        repository.tryAcquireLease(leased.id, "a", now.plusSeconds(30), now)
        ownerJdbc.sql("update transfer set next_attempt_at = null where id = :id").param("id", nullNext.id).update()

        assertThat(repository.findDue(now, 10)).containsExactly(due1.id, due2.id)
        assertThat(repository.findDue(now, 1)).containsExactly(due1.id)
    }

    @Test
    fun `findDue returns a transfer again once its lease expired`() {
        val now = microsNow()
        val transfer = aTransfer(createdAt = now.minusSeconds(30))
        repository.insert(transfer)
        repository.tryAcquireLease(transfer.id, "a", now.plusSeconds(30), now)

        assertThat(repository.findDue(now, 10)).isEmpty()
        assertThat(repository.findDue(now.plusSeconds(31), 10)).containsExactly(transfer.id)
    }

    /** Same boundary as tryAcquireLease (`locked_until < now`): a lease that ends exactly now still holds. */
    @Test
    fun `findDue does not return a transfer whose lease ends exactly now`() {
        val now = microsNow()
        val transfer = aTransfer(createdAt = now.minusSeconds(30))
        repository.insert(transfer)
        repository.tryAcquireLease(transfer.id, "a", now, now.minusSeconds(1))

        assertThat(repository.tryAcquireLease(transfer.id, "b", now.plusSeconds(30), now)).isFalse()
        assertThat(repository.findDue(now, 10)).isEmpty()
        assertThat(repository.findDue(now.plusNanos(1_000), 10)).containsExactly(transfer.id)
    }

    private fun nextAttemptAt(transfer: Transfer): Instant? =
        ownerJdbc
            .sql("select next_attempt_at from transfer where id = :id")
            .param("id", transfer.id)
            .query { rs, _ -> rs.getObject(1, OffsetDateTime::class.java)?.toInstant() }
            .optional()
            .orElse(null)

    private fun lockedUntil(transfer: Transfer): Instant? =
        ownerJdbc
            .sql("select locked_until from transfer where id = :id")
            .param("id", transfer.id)
            .query { rs, _ -> rs.getObject(1, OffsetDateTime::class.java)?.toInstant() }
            .optional()
            .orElse(null)

    private fun lockOwner(transfer: Transfer): String? =
        ownerJdbc
            .sql("select lock_owner from transfer where id = :id")
            .param("id", transfer.id)
            .query(String::class.java)
            .optional()
            .orElse(null)
}
