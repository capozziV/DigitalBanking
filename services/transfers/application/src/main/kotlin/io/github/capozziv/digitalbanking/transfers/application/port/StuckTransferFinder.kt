package io.github.capozziv.digitalbanking.transfers.application.port

import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import java.time.Instant

/** A transfer stuck in `Compensate`, with the last refusal of its compensation credit. */
data class StuckTransfer(
    val transfer: Transfer,
    val refusal: String?,
    val refusedAt: Instant,
)

/** Finds the transfers stuck in `Compensate` because their last compensation credit was refused. */
interface StuckTransferFinder {
    /** Oldest first, at most [limit], each with its transfer: one read, whatever the number of transfers. */
    fun findStuckCompensations(limit: Int): List<StuckTransfer>
}
