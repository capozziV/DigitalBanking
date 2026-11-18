package io.github.capozziv.digitalbanking.transfers.application.port

import io.github.capozziv.digitalbanking.transfers.domain.Money
import java.time.LocalDate
import java.util.UUID

enum class ReservationResult { RESERVED, EXCEEDED, UNAVAILABLE }

/** How much of an account's daily limit is reserved on [day]; [used] and [available] in cents. */
data class DailyLimitUsage(
    val day: LocalDate,
    val limit: Money,
    val used: Long,
    val available: Long,
)

/**
 * Daily limit of transfers per source account and São Paulo day. Each method runs its own database transaction, so it
 * must never be invoked inside one.
 */
interface DailyLimitGateway {
    /** Idempotent by transferId; holds the per-account-day lock; marks the transfer's limit_reserved on RESERVED. */
    fun reserve(
        transferId: UUID,
        accountId: UUID,
        day: LocalDate,
        amount: Money,
    ): ReservationResult

    /**
     * Gives the transfer's reservation back, after its failed or compensated state is saved (the source of truth already
     * stopped counting it). Best effort, never throws for a Redis failure: when the counter cannot be updated, the next
     * reservation of the account and day rebuilds it from the source of truth.
     */
    fun release(
        transferId: UUID,
        accountId: UUID,
        day: LocalDate,
    )

    /**
     * The reservations of the account on [day], for display: read from the source of truth without the lock of the
     * account and day, so it neither waits for nor delays reservations, and may miss one committing at that moment.
     */
    fun usage(
        accountId: UUID,
        day: LocalDate,
    ): DailyLimitUsage
}
