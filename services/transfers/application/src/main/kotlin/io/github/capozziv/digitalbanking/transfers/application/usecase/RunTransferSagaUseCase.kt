package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.port.AccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.port.CallResult
import io.github.capozziv.digitalbanking.transfers.application.port.EventOutbox
import io.github.capozziv.digitalbanking.transfers.application.port.StepRecord
import io.github.capozziv.digitalbanking.transfers.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.application.port.TransferStepLog
import io.github.capozziv.digitalbanking.transfers.domain.StepOutcome
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import io.github.capozziv.digitalbanking.transfers.domain.TransferStep
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Runs the transfer saga while [owner] holds the transfer's lease.
 *
 * Each step calls `accounts` outside any transaction, then records the result in one short transaction
 * (lease-guarded save, step record, terminal event). The loop stops on a terminal step, on a transient failure
 * (the scheduler retries at `next_attempt_at`), when a step does not change after a refusal, or when the lease is lost.
 */
class RunTransferSagaUseCase(
    private val transfers: TransferRepository,
    private val stepLog: TransferStepLog,
    private val accounts: AccountsGateway,
    private val outbox: EventOutbox,
    private val tx: TransactionRunner,
    private val backoff: RetryBackoff,
    private val settings: SagaSettings,
    private val clock: Clock,
) {
    /**
     * Returns the transfer as persisted by the last step, or `null` when the lease could not be taken,
     * the transfer does not exist, or the lease was lost mid-run (another instance now owns it; nothing more is written).
     *
     * [busyRetries] immediate repeats apply only to `503` transient failures; each call is logged in the step log
     * but only the final result advances the transfer's `attempts`.
     */
    fun execute(
        transferId: UUID,
        owner: String,
        busyRetries: Int = 0,
    ): Transfer? {
        val now = clock.instant()
        val acquired = tx.inTransaction { transfers.tryAcquireLease(transferId, owner, now.plus(settings.lease), now) }
        if (!acquired) return null
        try {
            return runSteps(transferId, owner, busyRetries)
        } finally {
            tx.inTransaction { transfers.releaseLease(transferId, owner) }
        }
    }

    private fun runSteps(
        transferId: UUID,
        owner: String,
        busyRetries: Int,
    ): Transfer? {
        var transfer = tx.inTransaction { transfers.findById(transferId) } ?: return null
        while (true) {
            val call = transfer.step.call(transfer) ?: return transfer
            var result = accounts.execute(call)
            var immediateRetries = 0
            while (isBusy(result) && immediateRetries < busyRetries) {
                val failed = result
                tx.inTransaction { stepLog.append(record(transfer, failed, clock.instant())) }
                immediateRetries++
                result = accounts.execute(call)
            }

            val at = clock.instant()
            val next = transfer.advance(result.outcome, at)
            val terminal = next.step is TransferStep.Terminal
            val stop = terminal || result.outcome is StepOutcome.TransientFailure || next.step == transfer.step
            val nextAttemptAt =
                if (stop && !terminal) at.plus(backoff.delayBeforeRetry(next.attempts.coerceAtLeast(1))) else null
            val event = next.terminalEvent()

            val saved =
                tx.inTransaction {
                    val owned = transfers.saveIfOwner(next, owner, nextAttemptAt)
                    if (owned) {
                        stepLog.append(record(transfer, result, at))
                        if (event != null) outbox.append(event)
                    }
                    owned
                }
            if (!saved) {
                LOG.log(System.Logger.Level.WARNING, "lease lost while running transfer {0}; stopping without effect", transferId)
                return null
            }
            logUnexpected(transfer, result)

            transfer = next
            if (stop) return transfer
        }
    }

    private fun isBusy(result: CallResult) = result.outcome is StepOutcome.TransientFailure && result.httpStatus == HTTP_SERVICE_UNAVAILABLE

    private fun logUnexpected(
        transfer: Transfer,
        result: CallResult,
    ) {
        val outcome = result.outcome
        val unexpected =
            outcome is StepOutcome.Refused &&
                (
                    transfer.step == TransferStep.Compensate ||
                        (
                            (transfer.step == TransferStep.ResolveDebit || transfer.step == TransferStep.ResolveCredit) &&
                                outcome.reason != StepOutcome.ENTRY_NOT_FOUND
                        )
                )
        if (unexpected) {
            LOG.log(
                System.Logger.Level.ERROR,
                "unexpected refusal in step {0} of transfer {1}: {2}",
                transfer.step.name,
                transfer.id,
                outcome.reason,
            )
        }
    }

    private fun record(
        transfer: Transfer,
        result: CallResult,
        at: Instant,
    ): StepRecord {
        val (name, reason) =
            when (val outcome = result.outcome) {
                StepOutcome.Success -> "SUCCESS" to null
                is StepOutcome.Refused -> "REFUSED" to outcome.reason
                StepOutcome.TransientFailure -> "TRANSIENT_FAILURE" to null
            }
        return StepRecord(transfer.id, transfer.step.name, name, result.httpStatus, result.detail ?: reason, at)
    }

    private companion object {
        const val HTTP_SERVICE_UNAVAILABLE = 503
        val LOG: System.Logger = System.getLogger(RunTransferSagaUseCase::class.java.name)
    }
}
