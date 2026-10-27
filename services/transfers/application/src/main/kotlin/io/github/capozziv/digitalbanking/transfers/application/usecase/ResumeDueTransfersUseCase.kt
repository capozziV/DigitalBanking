package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import java.time.Clock

/** One scheduler tick: runs the saga for each due transfer whose lease [owner] manages to take. */
class ResumeDueTransfersUseCase(
    private val transfers: TransferRepository,
    private val runSaga: RunTransferSagaUseCase,
    private val settings: SagaSettings,
    private val clock: Clock,
    private val owner: String,
) {
    /** Returns how many transfers this tick took (lease acquired and saga run). */
    fun execute(): Int =
        transfers
            .findDue(clock.instant(), settings.dueBatchSize)
            .count { runSaga.execute(it, owner) != null }
}
