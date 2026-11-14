package io.github.capozziv.digitalbanking.transfers.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
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
    /** Day of the source account's daily limit this transfer counts on: the São Paulo day of [createdAt], fixed. */
    val limitDay: LocalDate,
    /** True once the daily limit was reserved for this transfer; a failed or compensated transfer releases it. */
    val limitReserved: Boolean,
    /**
     * Customer that owned the source account when the transfer was requested, as `accounts` answered; only that
     * customer (or an admin) sees the transfer. Null for transfers requested before stage 7: those only an admin sees.
     */
    val sourceCustomerId: UUID? = null,
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
            when {
                next != TransferStep.Failed -> failureReason

                outcome is StepOutcome.Refused -> publicReason(outcome.reason)

                // Only ReserveLimit fails on a transient failure (past the deadline): the limit could not be checked.
                step == TransferStep.ReserveLimit -> StepOutcome.DAILY_LIMIT_UNAVAILABLE

                else -> failureReason
            }
        return copy(
            step = next,
            failureReason = reason,
            limitReserved = limitReserved || (step == TransferStep.ReserveLimit && outcome == StepOutcome.Success),
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
            sourceCustomerId: UUID,
            destinationAccountId: UUID,
            amount: Money,
            description: String?,
            idempotencyKey: String,
            correlationId: String?,
            now: Instant,
            deadlineAfter: Duration,
        ): Transfer {
            require(sourceAccountId != destinationAccountId) { "source and destination accounts must differ" }
            require(deadlineAfter.isPositive) { "deadlineAfter must be positive, was $deadlineAfter" }
            return Transfer(
                id = id,
                sourceAccountId = sourceAccountId,
                destinationAccountId = destinationAccountId,
                amount = amount,
                description = description,
                idempotencyKey = idempotencyKey,
                step = TransferStep.ReserveLimit,
                failureReason = null,
                deadline = now.plus(deadlineAfter),
                attempts = 0,
                correlationId = correlationId,
                createdAt = now,
                updatedAt = now,
                limitDay = DailyLimitDay.of(now),
                limitReserved = false,
                sourceCustomerId = sourceCustomerId,
            )
        }
    }
}
