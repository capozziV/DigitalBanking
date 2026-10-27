package io.github.capozziv.digitalbanking.transfers.application.port

import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import java.time.Instant
import java.util.UUID

interface TransferRepository {
    fun findById(id: UUID): Transfer?

    fun findByIdempotencyKey(key: String): Transfer?

    /** Returns false when the idempotency key already exists. */
    fun insert(transfer: Transfer): Boolean

    /** Takes the lease when it is free, expired at [now] or already held by [owner]. */
    fun tryAcquireLease(
        id: UUID,
        owner: String,
        until: Instant,
        now: Instant,
    ): Boolean

    /**
     * Saves [transfer] only if [owner] still holds the lease; returns false otherwise (nothing is written).
     * [nextAttemptAt] is when the scheduler may pick the transfer up again; null when no retry is scheduled
     * (terminal transfers, or a step that continues immediately). The domain `Transfer` does not carry it.
     */
    fun saveIfOwner(
        transfer: Transfer,
        owner: String,
        nextAttemptAt: Instant?,
    ): Boolean

    fun releaseLease(
        id: UUID,
        owner: String,
    )

    fun findDue(
        now: Instant,
        limit: Int,
    ): List<UUID>
}
