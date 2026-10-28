package io.github.capozziv.digitalbanking.transfers.bootstrap.persistence

import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import io.github.capozziv.digitalbanking.transfers.domain.TransferStep
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/** An instant with microsecond precision, which is what `timestamptz` stores. */
internal fun microsNow(): Instant = Instant.now().truncatedTo(ChronoUnit.MICROS)

internal fun aTransfer(
    createdAt: Instant = microsNow(),
    step: TransferStep = TransferStep.Debit,
    idempotencyKey: String = "key-${UUID.randomUUID()}",
    failureReason: String? = null,
    attempts: Int = 0,
): Transfer =
    Transfer(
        id = UUID.randomUUID(),
        sourceAccountId = UUID.randomUUID(),
        destinationAccountId = UUID.randomUUID(),
        amount = Money(12_345),
        description = "rent",
        idempotencyKey = idempotencyKey,
        step = step,
        failureReason = failureReason,
        deadline = createdAt.plusSeconds(120),
        attempts = attempts,
        correlationId = "corr-1",
        createdAt = createdAt,
        updatedAt = createdAt,
    )
