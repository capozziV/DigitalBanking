package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.port.CorrelationContext
import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitGateway
import io.github.capozziv.digitalbanking.transfers.application.port.EventOutbox
import io.github.capozziv.digitalbanking.transfers.application.port.ManualResolution
import io.github.capozziv.digitalbanking.transfers.application.port.ManualResolutionLog
import io.github.capozziv.digitalbanking.transfers.application.port.StuckTransferFinder
import io.github.capozziv.digitalbanking.transfers.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import io.github.capozziv.digitalbanking.transfers.domain.TransferStep
import java.time.Clock
import java.time.Instant
import java.util.UUID

/** A transfer stuck in `Compensate`, with the last refusal of its compensation credit. */
data class StuckTransfer(
    val transfer: Transfer,
    val refusal: String?,
    val refusedAt: Instant,
)

/** Admin: the transfers whose compensation `accounts` refused last time (a bug; the saga keeps retrying them). */
class ListStuckTransfersUseCase(
    private val finder: StuckTransferFinder,
    private val transfers: TransferRepository,
) {
    fun execute(limit: Int): List<StuckTransfer> =
        finder.findRefusedCompensations(limit).mapNotNull { refused ->
            transfers.findById(refused.transferId)?.let { StuckTransfer(it, refused.reason, refused.refusedAt) }
        }
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
}

/**
 * Admin: ends a transfer stuck in `Compensate` as `COMPENSATED` without the compensation credit, once the admin made
 * the source account whole outside the saga ([Transfer.compensateManually]). Takes the transfer's lease first, so it
 * never races a saga run ([CompensateManuallyOutcome.Busy]); then, in one transaction, saves the transfer as terminal,
 * records who, when and why ([ManualResolutionLog]) and writes the `TransferCompensated` event to the outbox. The daily
 * limit reservation is released afterwards, best effort, as the saga does. All of it runs under the transfer's
 * correlation ID. Repeating it answers the first resolution and writes nothing.
 */
class CompensateTransferManuallyUseCase(
    private val transfers: TransferRepository,
    private val resolutions: ManualResolutionLog,
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
    ): CompensateManuallyOutcome {
        val current = tx.inTransaction { transfers.findById(id) } ?: return CompensateManuallyOutcome.NotFound
        repeated(current)?.let { return it }
        if (current.step != TransferStep.Compensate) return CompensateManuallyOutcome.NotCompensating(current)
        return correlation.withCorrelationId(current.correlationId) { compensate(id, resolvedBy, reason) }
    }

    /** Under the transfer's correlation ID, like a saga run: its log lines and its event belong to the transfer. */
    private fun compensate(
        id: UUID,
        resolvedBy: UUID,
        reason: String,
    ): CompensateManuallyOutcome {
        val owner = newOwner()
        val now = clock.instant()
        val leaseUntil = now.plus(settings.lease)
        if (!tx.inTransaction { transfers.tryAcquireLease(id, owner, leaseUntil, now) }) return CompensateManuallyOutcome.Busy
        val outcome =
            try {
                tx.inTransaction { compensateUnderLease(id, owner, resolvedBy, reason, leaseUntil) }
            } finally {
                runCatching { tx.inTransaction { transfers.releaseLease(id, owner) } }
                    .onFailure { LOG.log(System.Logger.Level.WARNING, "could not release the lease of transfer $id", it) }
            }
        if (outcome is CompensateManuallyOutcome.Compensated) releaseLimitIfReserved(outcome.transfer)
        return outcome
    }

    /** Under the lease the transfer is read again: a saga run may have finished it between the first read and the lease. */
    private fun compensateUnderLease(
        id: UUID,
        owner: String,
        resolvedBy: UUID,
        reason: String,
        leaseUntil: Instant,
    ): CompensateManuallyOutcome {
        val transfer = transfers.findById(id) ?: return CompensateManuallyOutcome.NotFound
        repeated(transfer)?.let { return it }
        if (transfer.step != TransferStep.Compensate) return CompensateManuallyOutcome.NotCompensating(transfer)
        val now = clock.instant()
        val compensated = transfer.compensateManually(now)
        if (!transfers.saveIfOwner(compensated, owner, null, leaseUntil, now)) return CompensateManuallyOutcome.Busy
        val resolution = ManualResolution(id, resolvedBy, reason, now)
        resolutions.append(resolution)
        outbox.append(checkNotNull(compensated.terminalEvent()))
        LOG.log(
            System.Logger.Level.WARNING,
            "transfer {0} (correlation {1}) compensated by hand by admin {2}: {3}",
            id,
            transfer.correlationId,
            resolvedBy,
            reason,
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

    private companion object {
        val LOG: System.Logger = System.getLogger(CompensateTransferManuallyUseCase::class.java.name)
    }
}
