package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.RetryBackoff
import io.github.capozziv.digitalbanking.transfers.application.port.AccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.port.CallResult
import io.github.capozziv.digitalbanking.transfers.application.port.EventOutbox
import io.github.capozziv.digitalbanking.transfers.application.port.StepRecord
import io.github.capozziv.digitalbanking.transfers.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.application.port.TransferStepLog
import io.github.capozziv.digitalbanking.transfers.domain.AccountsCall
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
 * (lease-guarded save that also renews the lease, step records, terminal event). The loop stops on a terminal step,
 * on a transient failure (the scheduler retries at `next_attempt_at`), when a step does not change after a refusal,
 * when the lease is lost, or when too little of the lease is left to make another call safely.
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
     * A non-null return is also the persisted state when the run stops for lack of lease margin before a call
     * (nothing is written then; the scheduler resumes the transfer once the lease expires).
     *
     * [busyAttempts] is the total number of calls (the first included) a step makes in a row on `503` transient
     * failures; the default `1` means no immediate repeat. Every call is logged in the step log, but only the final
     * result advances the transfer's `attempts`.
     */
    fun execute(
        transferId: UUID,
        owner: String,
        busyAttempts: Int = 1,
    ): Transfer? {
        val now = clock.instant()
        val leaseUntil = now.plus(settings.lease)
        val acquired = tx.inTransaction { transfers.tryAcquireLease(transferId, owner, leaseUntil, now) }
        if (!acquired) return null
        try {
            return Run(transferId, owner, busyAttempts, leaseUntil).steps()
        } finally {
            runCatching { tx.inTransaction { transfers.releaseLease(transferId, owner) } }
                .onFailure { LOG.log(System.Logger.Level.WARNING, "could not release the lease of transfer $transferId", it) }
        }
    }

    private inner class Run(
        private val transferId: UUID,
        private val owner: String,
        private val busyAttempts: Int,
        private var leaseUntil: Instant,
    ) {
        fun steps(): Transfer? {
            var transfer = tx.inTransaction { transfers.findById(transferId) } ?: return null
            while (true) {
                val call = transfer.step.call(transfer) ?: return transfer
                val pending = mutableListOf<StepRecord>()
                var result = callIfLeaseAllows(transfer, call) ?: return transfer
                var attempts = 1
                while (isBusy(result) && attempts < busyAttempts) {
                    pending += record(transfer, result, clock.instant())
                    result = callIfLeaseAllows(transfer, call) ?: return transfer
                    attempts++
                }

                val at = clock.instant()
                val next = transfer.advance(result.outcome, at)
                val terminal = next.step is TransferStep.Terminal
                val retryLater = !terminal && (result.outcome is StepOutcome.TransientFailure || next.step == transfer.step)
                // A step that goes on right away is saved as due now, so the scheduler resumes it if this process dies.
                val nextAttemptAt =
                    when {
                        terminal -> null
                        retryLater -> at.plus(retryDelay(transfer, next, result))
                        else -> at
                    }
                val renewedUntil = at.plus(settings.lease)
                val event = next.terminalEvent()
                pending += record(transfer, result, at)

                val saved =
                    tx.inTransaction {
                        val owned = transfers.saveIfOwner(next, owner, nextAttemptAt, renewedUntil)
                        if (owned) {
                            pending.forEach(stepLog::append)
                            if (event != null) outbox.append(event)
                        }
                        owned
                    }
                if (!saved) {
                    LOG.log(
                        System.Logger.Level.WARNING,
                        "lease lost while running transfer {0} (correlation {1}); stopping without effect",
                        transferId,
                        transfer.correlationId,
                    )
                    return null
                }
                leaseUntil = renewedUntil
                logUnexpected(transfer, result)

                transfer = next
                if (terminal || retryLater) return transfer
            }
        }

        /** Calls `accounts` only while the worst-case call still ends inside the lease; null means stop without writing. */
        private fun callIfLeaseAllows(
            transfer: Transfer,
            call: AccountsCall,
        ): CallResult? {
            if (!clock.instant().isBefore(leaseUntil.minus(settings.callBudget))) {
                LOG.log(
                    System.Logger.Level.WARNING,
                    "lease of transfer {0} (correlation {1}) ends too soon for another call; stopping without effect",
                    transfer.id,
                    transfer.correlationId,
                )
                return null
            }
            return accounts.perform(call)
        }
    }

    private fun retryDelay(
        transfer: Transfer,
        next: Transfer,
        result: CallResult,
    ) = if (transfer.step == TransferStep.Compensate && result.outcome is StepOutcome.Refused) {
        backoff.max
    } else {
        // Known gap (M2, deferred): right after a step change attempts restarts, so the initial delay can repeat twice.
        backoff.delayBeforeRetry(next.attempts.coerceAtLeast(1))
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
                "unexpected refusal in step {0} of transfer {1} (correlation {2}): {3}",
                transfer.step.name,
                transfer.id,
                transfer.correlationId,
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
