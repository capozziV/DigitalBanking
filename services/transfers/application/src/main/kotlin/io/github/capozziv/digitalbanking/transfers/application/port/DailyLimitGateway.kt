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

    /** Best effort; never throws for a Redis failure. */
    fun release(
        transferId: UUID,
        accountId: UUID,
        day: LocalDate,
    )

    fun usage(
        accountId: UUID,
        day: LocalDate,
    ): DailyLimitUsage
}
