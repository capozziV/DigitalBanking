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
    /** Returns how many transfers this tick took (lease acquired and saga run to a stop without an exception). */
    fun execute(): Int =
        transfers
            .findDue(clock.instant(), settings.dueBatchSize)
            .count(::resume)

    private fun resume(transferId: UUID): Boolean {
        var correlationId: String? = null
        return runCatching {
            correlationId = transfers.findById(transferId)?.correlationId
            correlation.withCorrelationId(correlationId) { runSaga.execute(transferId, newOwner()) } != null
        }.getOrElse { error ->
            correlation.withCorrelationId(correlationId) {
                LOG.log(
                    System.Logger.Level.ERROR,
                    "saga of transfer $transferId (correlation $correlationId) failed; it is retried on a later tick",
                    error,
                )
            }
            false
        }
    }

    private companion object {
        val LOG: System.Logger = System.getLogger(ResumeDueTransfersUseCase::class.java.name)
    }
}
