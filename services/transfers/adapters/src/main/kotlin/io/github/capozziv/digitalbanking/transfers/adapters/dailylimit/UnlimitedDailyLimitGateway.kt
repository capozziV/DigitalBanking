package io.github.capozziv.digitalbanking.transfers.adapters.dailylimit

import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitGateway
import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitUsage
import io.github.capozziv.digitalbanking.transfers.application.port.ReservationResult
import io.github.capozziv.digitalbanking.transfers.domain.Money
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

/**
 * Temporary daily limit that never refuses: every reservation is accepted and nothing is counted.
 * It keeps the saga's `ReserveLimit` step running until the Redis + PostgreSQL gateway replaces it (stage 6, task 3).
 */
@Component
class UnlimitedDailyLimitGateway : DailyLimitGateway {
    override fun reserve(
        transferId: UUID,
        accountId: UUID,
        day: LocalDate,
        amount: Money,
    ): ReservationResult = ReservationResult.RESERVED

    override fun release(
        transferId: UUID,
        accountId: UUID,
        day: LocalDate,
    ) = Unit

    override fun usage(
        accountId: UUID,
        day: LocalDate,
    ): DailyLimitUsage = DailyLimitUsage(day, UNLIMITED, used = 0, available = UNLIMITED.cents)

    private companion object {
        val UNLIMITED = Money(Long.MAX_VALUE)
    }
}
