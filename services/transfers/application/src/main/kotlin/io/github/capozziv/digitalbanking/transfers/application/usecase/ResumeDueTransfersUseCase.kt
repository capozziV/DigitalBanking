package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.port.CorrelationContext
import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import java.time.Clock
import java.util.UUID

/**
 * One scheduler tick: runs the saga for each due transfer whose lease it manages to take.
 *
 * Each run takes the lease under a fresh owner from [newOwner]: `tryAcquireLease` is reentrant for the same owner, so
 * a fixed owner would let a tick take over a lease that an earlier, still running run of the same process holds.
 * Each saga runs under the transfer's own correlation ID, so its logs, its calls to `accounts` and its events carry
 * the ID of the request that created it. An exception in one transfer is logged and the batch goes on.
 */
class ResumeDueTransfersUseCase(
    private val transfers: TransferRepository,
    private val runSaga: RunTransferSagaUseCase,
    private val settings: SagaSettings,
    private val clock: Clock,
    private val correlation: CorrelationContext,
    private val newOwner: () -> String,
) {
    /**
     * Returns how many transfers this tick took (lease acquired and saga run to a stop without an exception).
     * [shouldContinue] is checked before each transfer: once it is false the rest of the batch is left for a later
     * tick (shutdown). An interruption ends the batch too, with the interrupt flag restored.
     */
    fun execute(shouldContinue: () -> Boolean = { true }): Int {
        var taken = 0
        for (transferId in transfers.findDue(clock.instant(), settings.dueBatchSize)) {
            if (!shouldContinue()) break
            when (resume(transferId)) {
                Outcome.TAKEN -> taken++
                Outcome.SKIPPED -> Unit
                Outcome.INTERRUPTED -> break
            }
        }
        return taken
    }

    private enum class Outcome { TAKEN, SKIPPED, INTERRUPTED }

    private fun resume(transferId: UUID): Outcome {
        var correlationId: String? = null
        return try {
            correlationId = transfers.findById(transferId)?.correlationId
            val ran = correlation.withCorrelationId(correlationId) { runSaga.execute(transferId, newOwner()) }
            if (ran != null) Outcome.TAKEN else Outcome.SKIPPED
        } catch (error: Exception) {
            if (error.causedByInterruption()) {
                Thread.currentThread().interrupt()
                LOG.log(System.Logger.Level.WARNING, "batch interrupted at transfer $transferId; stopping it")
                return Outcome.INTERRUPTED
            }
            correlation.withCorrelationId(correlationId) {
                LOG.log(
                    System.Logger.Level.ERROR,
                    "saga of transfer $transferId (correlation $correlationId) failed; it is retried on a later tick",
                    error,
                )
            }
            Outcome.SKIPPED
        }
    }

    private fun Throwable.causedByInterruption(): Boolean =
        generateSequence(this) { it.cause?.takeIf { cause -> cause !== it } }.take(MAX_CAUSE_DEPTH).any { it is InterruptedException }

    private companion object {
        const val MAX_CAUSE_DEPTH = 10
        val LOG: System.Logger = System.getLogger(ResumeDueTransfersUseCase::class.java.name)
    }
}
