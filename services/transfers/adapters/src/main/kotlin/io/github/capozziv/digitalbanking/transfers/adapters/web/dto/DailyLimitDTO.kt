package io.github.capozziv.digitalbanking.transfers.adapters.web.dto

import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitUsage

/** The daily limit of an account on [day] (São Paulo calendar day, `yyyy-MM-dd`); every amount in cents. */
data class DailyLimitDTO(
    val day: String,
    val limitCents: Long,
    val usedCents: Long,
    val availableCents: Long,
) {
    companion object {
        fun from(usage: DailyLimitUsage) =
            DailyLimitDTO(
                day = usage.day.toString(),
                limitCents = usage.limit.cents,
                usedCents = usage.used,
                availableCents = usage.available,
            )
    }
}
