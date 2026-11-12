package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryDailyLimitGateway
import io.github.capozziv.digitalbanking.transfers.application.fake.MutableClock
import io.github.capozziv.digitalbanking.transfers.domain.DailyLimitDay
import io.github.capozziv.digitalbanking.transfers.domain.Money
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class GetDailyLimitUseCaseTest {
    private val account = UUID.randomUUID()
    private val limits = InMemoryDailyLimitGateway(Money(500_000))

    @Test
    fun `reports the usage of today in Sao Paulo, not in UTC`() {
        // 02:30 UTC of the 13th is 23:30 of the 12th in São Paulo.
        val clock = MutableClock(Instant.parse("2026-11-13T02:30:00Z"))
        val today = LocalDate.parse("2026-11-12")
        limits.reserve(UUID.randomUUID(), account, today, Money(300_000))
        limits.reserve(UUID.randomUUID(), account, today.plusDays(1), Money(100_000))

        val usage = GetDailyLimitUseCase(limits, clock).execute(account)

        assertThat(usage.day).isEqualTo(today)
        assertThat(usage.limit).isEqualTo(Money(500_000))
        assertThat(usage.used).isEqualTo(300_000)
        assertThat(usage.available).isEqualTo(200_000)
    }

    @Test
    fun `an account that never transferred has nothing used`() {
        val clock = MutableClock(Instant.parse("2026-11-12T15:00:00Z"))

        val usage = GetDailyLimitUseCase(limits, clock).execute(UUID.randomUUID())

        assertThat(usage.day).isEqualTo(DailyLimitDay.of(clock.instant()))
        assertThat(usage.used).isZero()
        assertThat(usage.available).isEqualTo(500_000)
    }
}
