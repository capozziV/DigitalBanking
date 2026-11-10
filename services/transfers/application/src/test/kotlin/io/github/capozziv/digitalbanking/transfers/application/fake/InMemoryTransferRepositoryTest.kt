package io.github.capozziv.digitalbanking.transfers.application.fake

import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID

class InMemoryTransferRepositoryTest {
    private val id = UUID.fromString("00000000-0000-7000-8000-000000000001")
    private val createdAt = Instant.parse("2026-10-27T12:00:00Z")
    private val repository = InMemoryTransferRepository()

    private fun insertNew() {
        val transfer =
            Transfer.request(
                id = id,
                sourceAccountId = UUID.fromString("00000000-0000-7000-8000-0000000000a1"),
                destinationAccountId = UUID.fromString("00000000-0000-7000-8000-0000000000b2"),
                amount = Money(500),
                description = "rent",
                idempotencyKey = "key-1",
                correlationId = "corr-1",
                now = createdAt,
                deadlineAfter = Duration.ofMinutes(2),
            )
        repository.insert(transfer)
    }

    @Test
    fun `a transfer inserted and never executed is due from its creation instant`() {
        insertNew()

        assertThat(repository.nextAttemptAt(id)).isEqualTo(createdAt)
        assertThat(repository.findDue(createdAt.minusSeconds(1), 10)).isEmpty()
        assertThat(repository.findDue(createdAt, 10)).containsExactly(id)
        assertThat(repository.findDue(createdAt.plusSeconds(60), 10)).containsExactly(id)
    }

    @Test
    fun `a newly inserted transfer held by a valid lease is not due`() {
        insertNew()
        repository.holdLease(id, "other", createdAt.plusSeconds(30))

        assertThat(repository.findDue(createdAt.plusSeconds(1), 10)).isEmpty()
        assertThat(repository.findDue(createdAt.plusSeconds(31), 10)).containsExactly(id)
    }

    @Test
    fun `saveIfOwner refuses when the lease of the owner has expired`() {
        insertNew()
        assertThat(repository.tryAcquireLease(id, "a", createdAt.plusSeconds(30), createdAt)).isTrue()
        val transfer = repository.findById(id)!!
        val late = createdAt.plusSeconds(31)

        assertThat(repository.saveIfOwner(transfer, "a", null, late.plusSeconds(30), late)).isFalse()
        assertThat(repository.saveIfOwner(transfer, "a", null, createdAt.plusSeconds(60), createdAt.plusSeconds(30))).isTrue()
    }
}
