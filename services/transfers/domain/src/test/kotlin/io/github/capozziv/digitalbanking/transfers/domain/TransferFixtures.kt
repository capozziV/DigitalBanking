package io.github.capozziv.digitalbanking.transfers.domain

import java.time.Duration
import java.time.Instant
import java.util.UUID

object TransferFixtures {
    val id: UUID = UUID.fromString("00000000-0000-7000-8000-000000000001")
    val source: UUID = UUID.fromString("00000000-0000-7000-8000-0000000000a1")
    val destination: UUID = UUID.fromString("00000000-0000-7000-8000-0000000000b2")
    val created: Instant = Instant.parse("2026-10-27T12:00:00Z")
    val beforeDeadline: Instant = created.plusSeconds(10)
    val afterDeadline: Instant = created.plus(Duration.ofMinutes(2)).plusSeconds(1)

    fun transfer(step: TransferStep = TransferStep.Debit): Transfer =
        Transfer
            .request(
                id = id,
                sourceAccountId = source,
                destinationAccountId = destination,
                amount = Money(500),
                description = "rent",
                idempotencyKey = "key-1",
                correlationId = "corr-1",
                now = created,
                deadlineAfter = Duration.ofMinutes(2),
            ).copy(step = step)
}
