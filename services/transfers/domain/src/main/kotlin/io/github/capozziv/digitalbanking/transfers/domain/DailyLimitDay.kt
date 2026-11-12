package io.github.capozziv.digitalbanking.transfers.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The daily limit counts by calendar day in São Paulo, whatever the UTC day of the instant. */
object DailyLimitDay {
    val ZONE: ZoneId = ZoneId.of("America/Sao_Paulo")

    fun of(instant: Instant): LocalDate = LocalDate.ofInstant(instant, ZONE)
}
