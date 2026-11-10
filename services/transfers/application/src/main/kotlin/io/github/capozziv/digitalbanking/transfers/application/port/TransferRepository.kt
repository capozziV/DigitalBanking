package io.github.capozziv.digitalbanking.transfers.application.port

import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import java.time.Instant
import java.util.UUID

interface TransferRepository {
    fun findById(id: UUID): Transfer?

    fun findByIdempotencyKey(key: String): Transfer?

    /**
     * Returns false when the idempotency key already exists. A new row is written with
     * `next_attempt_at = transfer.createdAt`, so the scheduler finds it even if the process dies before the first
     * [saveIfOwner].
     */
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
     * The lease must be valid at [now] (`locked_until >= now`, symmetric to [tryAcquireLease]); the same write renews it
     * to [leaseUntil] (`locked_until`).
     *
     * [nextAttemptAt] is when the scheduler may pick the transfer up again; the domain `Transfer` does not carry it.
     * It is null only for a terminal transfer. A non-terminal transfer always gets a value, even when the saga goes on
     * right away (then it is the save instant): the lease keeps other instances away while it holds, and if the process
     * dies before the next save the scheduler resumes the transfer once the lease expires.
     */
    fun saveIfOwner(
        transfer: Transfer,
        owner: String,
        nextAttemptAt: Instant?,
        leaseUntil: Instant,
        now: Instant,
    ): Boolean

    fun releaseLease(
        id: UUID,
        owner: String,
    )

    /** Pending transfers with `nextAttemptAt <= now` and no valid lease; a null `nextAttemptAt` is never due. */
    fun findDue(
        now: Instant,
        limit: Int,
    ): List<UUID>
}
