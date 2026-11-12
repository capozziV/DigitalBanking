package io.github.capozziv.digitalbanking.transfers.adapters.dailylimit

import io.github.capozziv.digitalbanking.transfers.application.port.ReservationResult
import io.github.capozziv.digitalbanking.transfers.domain.Money
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class UnlimitedDailyLimitGatewayTest {
    private val gateway = UnlimitedDailyLimitGateway()
    private val account = UUID.randomUUID()
    private val day = LocalDate.parse("2026-11-12")

    @Test
    fun `every reservation is accepted, however large`() {
        assertThat(gateway.reserve(UUID.randomUUID(), account, day, Money(Long.MAX_VALUE))).isEqualTo(ReservationResult.RESERVED)
        assertThat(gateway.reserve(UUID.randomUUID(), account, day, Money(Long.MAX_VALUE))).isEqualTo(ReservationResult.RESERVED)
    }

    @Test
    fun `usage is always zero`() {
        gateway.reserve(UUID.randomUUID(), account, day, Money(500))
        gateway.release(UUID.randomUUID(), account, day)

        val usage = gateway.usage(account, day)

        assertThat(usage.day).isEqualTo(day)
        assertThat(usage.used).isZero()
        assertThat(usage.available).isEqualTo(usage.limit.cents)
    }
}
