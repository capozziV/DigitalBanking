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

    /**
     * Why [key] cannot name the compensation credit, or `null` when it can. A key in the saga's pattern
     * (`transfer-<id>-debit|credit|compensation`) belongs to the transfer in its `<id>`: the credit of another saga
     * (its compensation, or its destination credit) never proves this one was put back. Among the saga keys only this
     * transfer's own compensation key is accepted, being the credit the saga itself would write; the `<id>` is
     * compared as a UUID, so letter case does not matter.
     */
    fun manualCreditKeyMismatch(key: String): String? {
        val match = SAGA_KEY.matchEntire(key) ?: return null
        val (owner, step) = match.destructured
        val own = runCatching { UUID.fromString(owner) }.getOrNull() == id
        return when {
            !own -> "the key is a saga key of another transfer"
            step != "compensation" -> "the key is the $step key of this transfer, not its compensation credit"
            else -> null
        }
    }

    /**
     * Why [entry] cannot stand for the compensation credit, or null when it can: a credit of exactly [amount] on the
     * source account, which puts back what the debit took.
     */
    fun manualCreditMismatch(entry: RecordedEntry): String? =
        when {
            entry.accountId != sourceAccountId -> {
                "the entry is on account ${entry.accountId}, not the source account $sourceAccountId"
            }

            entry.kind != EntryKind.CREDIT -> {
                "the entry is a ${entry.kind.name}, not a ${EntryKind.CREDIT.name}"
            }

            entry.amount != amount -> {
                "the entry is of ${entry.amount.cents} cents, the transfer of ${amount.cents} cents"
            }

            else -> {
                null
            }
        }

    /**
     * Ends a transfer stuck in [TransferStep.Compensate] as [TransferStep.Compensated] with a credit the admin recorded
     * outside the saga (the saga's credit kept being refused, a bug). The value went back to the source only if [credit]
     * is the compensation credit ([manualCreditMismatch]); otherwise an [IllegalArgumentException]. Only `Compensate`
     * qualifies; any other step is an [IllegalStateException].
     */
    fun compensateManually(
        credit: RecordedEntry,
        now: Instant,
    ): Transfer {
        check(step == TransferStep.Compensate) { "transfer $id is in step ${step.name}, not ${TransferStep.Compensate.name}" }
        manualCreditMismatch(credit)?.let { throw IllegalArgumentException("transfer $id: $it") }
        return copy(step = TransferStep.Compensated, attempts = 0, updatedAt = now)
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
        private val SAGA_KEY = Regex("transfer-(.+)-(debit|credit|compensation)")

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
