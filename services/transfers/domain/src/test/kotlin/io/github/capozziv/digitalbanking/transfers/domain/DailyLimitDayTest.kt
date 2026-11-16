package io.github.capozziv.digitalbanking.transfers.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DailyLimitDayTest {
    @Test
    fun `the limit day is the Sao Paulo calendar day`() {
        assertThat(DailyLimitDay.ZONE).isEqualTo(ZoneId.of("America/Sao_Paulo"))
        assertThat(DailyLimitDay.of(Instant.parse("2026-11-13T02:59:59Z"))).isEqualTo(LocalDate.parse("2026-11-12"))
        assertThat(DailyLimitDay.of(Instant.parse("2026-11-13T03:00:00Z"))).isEqualTo(LocalDate.parse("2026-11-13"))
    }

    @Test
    fun `a transfer requested at 23h59 in Sao Paulo counts on that Sao Paulo day, not the UTC day`() {
        val transfer =
            Transfer.request(
                id = TransferFixtures.id,
                sourceAccountId = TransferFixtures.source,
                sourceCustomerId = TransferFixtures.sourceCustomer,
                destinationAccountId = TransferFixtures.destination,
                amount = Money(1),
                description = null,
                idempotencyKey = "k",
                correlationId = null,
                now = Instant.parse("2026-11-13T02:59:00Z"),
                deadlineAfter = Duration.ofMinutes(2),
            )

        assertThat(transfer.limitDay).isEqualTo(LocalDate.parse("2026-11-12"))
    }
}
