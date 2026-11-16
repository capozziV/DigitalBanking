package io.github.capozziv.digitalbanking.transfers.application.port

import java.time.Instant
import java.util.UUID

/** An admin ended a stuck transfer by hand: who, when and why. */
data class ManualResolution(
    val transferId: UUID,
    val resolvedBy: UUID,
    val reason: String,
    val resolvedAt: Instant,
)

/** Audit trail of the manual resolutions; at most one per transfer. Written in the caller's transaction. */
interface ManualResolutionLog {
    fun append(resolution: ManualResolution)

    fun find(transferId: UUID): ManualResolution?
}

/** A pending transfer whose last compensation credit `accounts` refused, with that refusal. */
data class RefusedCompensation(
    val transferId: UUID,
    val reason: String?,
    val refusedAt: Instant,
)

/** Finds the transfers stuck in `Compensate` because their last compensation credit was refused. */
interface StuckTransferFinder {
    /** Oldest first, at most [limit]. */
    fun findRefusedCompensations(limit: Int): List<RefusedCompensation>
}
