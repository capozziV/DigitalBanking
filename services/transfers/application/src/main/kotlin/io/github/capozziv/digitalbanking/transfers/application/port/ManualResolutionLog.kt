package io.github.capozziv.digitalbanking.transfers.application.port

import java.time.Instant
import java.util.UUID

/**
 * An admin ended a stuck transfer by hand: who, when, why and with which credit of `accounts` ([creditIdempotencyKey],
 * the key the compensation credit was recorded under on the source account).
 */
data class ManualResolution(
    val transferId: UUID,
    val resolvedBy: UUID,
    val reason: String,
    val creditIdempotencyKey: String,
    val resolvedAt: Instant,
)

/** Audit trail of the manual resolutions; at most one per transfer and one per credit. Written in the caller's transaction. */
interface ManualResolutionLog {
    /**
     * Records [resolution]; false, writing nothing, when its credit already resolved another transfer (one credit puts
     * back the value of one transfer only).
     */
    fun append(resolution: ManualResolution): Boolean

    fun find(transferId: UUID): ManualResolution?
}
