package io.github.capozziv.digitalbanking.transfers.domain

import java.time.Duration
import java.time.Instant
import java.util.UUID

data class Transfer(
    val id: UUID,
    val sourceAccountId: UUID,
    val destinationAccountId: UUID,
    val amount: Money,
    val description: String?,
    val idempotencyKey: String,
    val step: TransferStep,
    val failureReason: String?,
    val deadline: Instant,
    val attempts: Int,
    val correlationId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val status: TransferStatus get() = step.status

    fun isPastDeadline(now: Instant): Boolean = !now.isBefore(deadline)

    /** Applies the [outcome] of the current step's call and returns the transfer in its next step. */
    fun advance(
        outcome: StepOutcome,
        now: Instant,
    ): Transfer {
        val next =
            when (outcome) {
                StepOutcome.Success -> step.onSuccess(this)
                is StepOutcome.Refused -> step.onRefused(this, outcome.reason)
                StepOutcome.TransientFailure -> step.onTransientFailure(this, now)
            }
        val reason =
            if (outcome is StepOutcome.Refused && next == TransferStep.Failed) publicReason(outcome.reason) else failureReason
        return copy(
            step = next,
            failureReason = reason,
            attempts = if (next == step) attempts + 1 else 0,
            updatedAt = now,
        )
    }

    /** `entry-not-found` is internal to the resolve step; the API and the event say the debit was not confirmed. */
    private fun publicReason(reason: String): String =
        if (step == TransferStep.ResolveDebit && reason == StepOutcome.ENTRY_NOT_FOUND) StepOutcome.DEBIT_NOT_CONFIRMED else reason

    fun terminalEvent(): TransferEvent? =
        when (step) {
            TransferStep.Completed -> {
                TransferEvent.TransferCompleted(id, sourceAccountId, destinationAccountId, amount, description)
            }

            TransferStep.Failed -> {
                TransferEvent.TransferFailed(
                    id,
                    sourceAccountId,
                    destinationAccountId,
                    amount,
                    description,
                    checkNotNull(failureReason) { "failed transfer $id has no failure reason" },
                )
            }

            TransferStep.Compensated -> {
                TransferEvent.TransferCompensated(id, sourceAccountId, destinationAccountId, amount, description)
            }

            else -> {
                null
            }
        }

    companion object {
        fun request(
            id: UUID,
            sourceAccountId: UUID,
            destinationAccountId: UUID,
            amount: Money,
            description: String?,
            idempotencyKey: String,
            correlationId: String?,
            now: Instant,
            deadlineAfter: Duration,
        ): Transfer {
            require(sourceAccountId != destinationAccountId) { "source and destination accounts must differ" }
            return Transfer(
                id = id,
                sourceAccountId = sourceAccountId,
                destinationAccountId = destinationAccountId,
                amount = amount,
                description = description,
                idempotencyKey = idempotencyKey,
                step = TransferStep.Debit,
                failureReason = null,
                deadline = now.plus(deadlineAfter),
                attempts = 0,
                correlationId = correlationId,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
