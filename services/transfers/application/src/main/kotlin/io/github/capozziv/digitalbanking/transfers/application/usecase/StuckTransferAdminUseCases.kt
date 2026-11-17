package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.port.AccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.port.CorrelationContext
import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitGateway
import io.github.capozziv.digitalbanking.transfers.application.port.EntryLookup
import io.github.capozziv.digitalbanking.transfers.application.port.EventOutbox
import io.github.capozziv.digitalbanking.transfers.application.port.ManualResolution
import io.github.capozziv.digitalbanking.transfers.application.port.ManualResolutionLog
import io.github.capozziv.digitalbanking.transfers.application.port.StuckTransfer
import io.github.capozziv.digitalbanking.transfers.application.port.StuckTransferFinder
import io.github.capozziv.digitalbanking.transfers.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.domain.RecordedEntry
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import io.github.capozziv.digitalbanking.transfers.domain.TransferStep
import java.time.Clock
import java.time.Instant
import java.util.UUID

/** Admin: the transfers whose compensation `accounts` refused last time (a bug; the saga keeps retrying them). */
class ListStuckTransfersUseCase(
    private val finder: StuckTransferFinder,
) {
    fun execute(limit: Int): List<StuckTransfer> = finder.findStuckCompensations(limit)
}

sealed interface RetryTransferOutcome {
    data class Ran(
        val transfer: Transfer,
    ) : RetryTransferOutcome

    data object NotFound : RetryTransferOutcome

    data class NotPending(
        val transfer: Transfer,
    ) : RetryTransferOutcome

    data object Busy : RetryTransferOutcome
}

/**
 * Admin: runs the saga of a pending transfer now, in the request, instead of waiting for its next attempt (a refused
 * compensation waits the longest backoff). Same run as the scheduler's: under the transfer's correlation ID and its
 * lease, with a fresh owner, so it never runs alongside another run ([RetryTransferOutcome.Busy] then).
 */
class RetryTransferUseCase(
    private val transfers: TransferRepository,
    private val runSaga: RunTransferSagaUseCase,
    private val correlation: CorrelationContext,
    private val newOwner: () -> String,
) {
    fun execute(id: UUID): RetryTransferOutcome {
        val transfer = transfers.findById(id) ?: return RetryTransferOutcome.NotFound
        if (transfer.step is TransferStep.Terminal) return RetryTransferOutcome.NotPending(transfer)
        val ran = correlation.withCorrelationId(transfer.correlationId) { runSaga.execute(id, newOwner()) }
        return if (ran != null) RetryTransferOutcome.Ran(ran) else RetryTransferOutcome.Busy
    }
}

sealed interface CompensateManuallyOutcome {
    /** [repeated] is true when the transfer had already been compensated by hand: nothing was written this time. */
    data class Compensated(
        val transfer: Transfer,
        val resolution: ManualResolution,
        val repeated: Boolean,
    ) : CompensateManuallyOutcome

    data object NotFound : CompensateManuallyOutcome

    data class NotCompensating(
        val transfer: Transfer,
    ) : CompensateManuallyOutcome

    data object Busy : CompensateManuallyOutcome

    /** `accounts` has no entry under [creditIdempotencyKey] on the source account: nothing proves the value went back. */
    data class CreditNotFound(
        val creditIdempotencyKey: String,
    ) : CompensateManuallyOutcome

    /** The entry under the key is not the compensation credit ([Transfer.manualCreditMismatch] says why). */
    data class CreditMismatch(
        val reason: String,
    ) : CompensateManuallyOutcome

    /** The credit under [creditIdempotencyKey] already compensated another transfer by hand. */
    data class CreditAlreadyUsed(
        val creditIdempotencyKey: String,
    ) : CompensateManuallyOutcome

    /** `accounts` did not answer, so the credit could not be checked; try again later. */
    data object AccountsUnavailable : CompensateManuallyOutcome
}

/**
 * Admin: ends a transfer stuck in `Compensate` as `COMPENSATED` with a credit the admin recorded on the source account
 * outside the saga ([Transfer.compensateManually]). The admin's word is not enough: the credit is read back from
 * `accounts` by its idempotency key, outside any transaction, and must be a credit of the transfer's amount on the
 * source account; otherwise the transfer stays stuck and nothing is written. Then the use case takes the transfer's
 * lease, so it never races a saga run ([CompensateManuallyOutcome.Busy]), and in one transaction records who, when, why
 * and which credit ([ManualResolutionLog], one transfer per credit), saves the transfer as terminal and writes the
 * `TransferCompensated` event to the outbox. The daily limit reservation is released afterwards, best effort, as the
 * saga does. All of it runs under the transfer's correlation ID. Repeating it answers the first resolution and writes
 * nothing.
 */
class CompensateTransferManuallyUseCase(
    private val transfers: TransferRepository,
    private val resolutions: ManualResolutionLog,
    private val accounts: AccountsGateway,
    private val outbox: EventOutbox,
    private val dailyLimits: DailyLimitGateway,
    private val tx: TransactionRunner,
    private val settings: SagaSettings,
    private val correlation: CorrelationContext,
    private val clock: Clock,
    private val newOwner: () -> String,
) {
    fun execute(
        id: UUID,
        resolvedBy: UUID,
        reason: String,
        creditIdempotencyKey: String,
    ): CompensateManuallyOutcome {
        val current = tx.inTransaction { transfers.findById(id) } ?: return CompensateManuallyOutcome.NotFound
        repeated(current)?.let { return it }
        if (current.step != TransferStep.Compensate) return CompensateManuallyOutcome.NotCompensating(current)
        return correlation.withCorrelationId(current.correlationId) {
            when (val credit = checkCredit(current, creditIdempotencyKey)) {
                is CreditCheck.Rejected -> credit.outcome
                is CreditCheck.Accepted -> compensate(id, ManualRequest(resolvedBy, reason, creditIdempotencyKey, credit.entry))
            }
        }
    }

    /** Reads the credit back from `accounts` (never inside a transaction) and checks it puts back the transfer's value. */
    private fun checkCredit(
        transfer: Transfer,
        key: String,
    ): CreditCheck =
        when (val lookup = accounts.findEntry(transfer.sourceAccountId, key)) {
            EntryLookup.NotFound -> {
                CreditCheck.Rejected(CompensateManuallyOutcome.CreditNotFound(key))
            }

            EntryLookup.Unavailable -> {
                CreditCheck.Rejected(CompensateManuallyOutcome.AccountsUnavailable)
            }

            is EntryLookup.Found -> {
                transfer.manualCreditMismatch(lookup.entry)?.let { CreditCheck.Rejected(CompensateManuallyOutcome.CreditMismatch(it)) }
                    ?: CreditCheck.Accepted(lookup.entry)
            }
        }

    /** Under the transfer's correlation ID, like a saga run: its log lines and its event belong to the transfer. */
    private fun compensate(
        id: UUID,
        request: ManualRequest,
    ): CompensateManuallyOutcome {
        val owner = newOwner()
        val now = clock.instant()
        val leaseUntil = now.plus(settings.lease)
        if (!tx.inTransaction { transfers.tryAcquireLease(id, owner, leaseUntil, now) }) return CompensateManuallyOutcome.Busy
        val outcome =
            try {
                tx.inTransaction { compensateUnderLease(id, owner, request, leaseUntil) }
            } catch (_: LeaseLostException) {
                CompensateManuallyOutcome.Busy
            } finally {
                runCatching { tx.inTransaction { transfers.releaseLease(id, owner) } }
                    .onFailure { LOG.log(System.Logger.Level.WARNING, "could not release the lease of transfer $id", it) }
            }
        if (outcome is CompensateManuallyOutcome.Compensated) releaseLimitIfReserved(outcome.transfer)
        return outcome
    }

    /**
     * Under the lease the transfer is read again: a saga run may have finished it between the first read and the lease.
     * The resolution goes first, so a credit already used writes nothing; a lease lost afterwards throws
     * [LeaseLostException] to roll the resolution back.
     */
    private fun compensateUnderLease(
        id: UUID,
        owner: String,
        request: ManualRequest,
        leaseUntil: Instant,
    ): CompensateManuallyOutcome {
        val transfer = transfers.findById(id) ?: return CompensateManuallyOutcome.NotFound
        repeated(transfer)?.let { return it }
        if (transfer.step != TransferStep.Compensate) return CompensateManuallyOutcome.NotCompensating(transfer)
        val now = clock.instant()
        val compensated = transfer.compensateManually(request.credit, now)
        val resolution = ManualResolution(id, request.resolvedBy, request.reason, request.creditIdempotencyKey, now)
        if (!resolutions.append(resolution)) return CompensateManuallyOutcome.CreditAlreadyUsed(request.creditIdempotencyKey)
        if (!transfers.saveIfOwner(compensated, owner, null, leaseUntil, now)) throw LeaseLostException()
        outbox.append(checkNotNull(compensated.terminalEvent()))
        LOG.log(
            System.Logger.Level.WARNING,
            "transfer {0} (correlation {1}) compensated by hand by admin {2} with credit {3}: {4}",
            id,
            transfer.correlationId,
            request.resolvedBy,
            request.creditIdempotencyKey,
            request.reason,
        )
        return CompensateManuallyOutcome.Compensated(compensated, resolution, repeated = false)
    }

    private fun repeated(transfer: Transfer): CompensateManuallyOutcome? =
        if (transfer.step == TransferStep.Compensated) {
            resolutions.find(transfer.id)?.let { CompensateManuallyOutcome.Compensated(transfer, it, repeated = true) }
        } else {
            null
        }

    private fun releaseLimitIfReserved(transfer: Transfer) {
        if (!transfer.limitReserved) return
        runCatching { dailyLimits.release(transfer.id, transfer.sourceAccountId, transfer.limitDay) }
            .onFailure {
                LOG.log(
                    System.Logger.Level.WARNING,
                    "could not release the daily limit of transfer ${transfer.id} (correlation ${transfer.correlationId})",
                    it,
                )
            }
    }

    private class ManualRequest(
        val resolvedBy: UUID,
        val reason: String,
        val creditIdempotencyKey: String,
        val credit: RecordedEntry,
    )

    private sealed interface CreditCheck {
        class Accepted(
            val entry: RecordedEntry,
        ) : CreditCheck

        class Rejected(
            val outcome: CompensateManuallyOutcome,
        ) : CreditCheck
    }

    /** The lease ran out between taking it and saving: thrown to roll back the resolution already written. */
    private class LeaseLostException : RuntimeException()

    private companion object {
        val LOG: System.Logger = System.getLogger(CompensateTransferManuallyUseCase::class.java.name)
    }
}
