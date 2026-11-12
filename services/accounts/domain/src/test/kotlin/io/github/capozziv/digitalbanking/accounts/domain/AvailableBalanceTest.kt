package io.github.capozziv.digitalbanking.accounts.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AvailableBalanceTest {
    private val negativeBalance = Balance(-50)
    private val limit = OverdraftLimit(200)
    private val nearMaximum = Balance(Long.MAX_VALUE - 10)

    @Test
    fun `available balance is the balance plus the overdraft limit`() {
        assertThat(AvailableBalance.of(negativeBalance, limit)).isEqualTo(AvailableBalance(150))
    }

    @Test
    fun `available balance beyond the maximum saturates at Long MAX_VALUE instead of wrapping around`() {
        assertThat(AvailableBalance.of(Balance(Long.MAX_VALUE), OverdraftLimit(1))).isEqualTo(AvailableBalance(Long.MAX_VALUE))
        assertThat(AvailableBalance.of(nearMaximum, OverdraftLimit(5_000_000))).isEqualTo(AvailableBalance(Long.MAX_VALUE))
    }

    @Test
    fun `available balance just below the maximum is exact`() {
        assertThat(AvailableBalance.of(nearMaximum, OverdraftLimit(10))).isEqualTo(AvailableBalance(Long.MAX_VALUE))
        assertThat(AvailableBalance.of(nearMaximum, OverdraftLimit(9))).isEqualTo(AvailableBalance(Long.MAX_VALUE - 1))
    }

    @Test
    fun `available balance covers an amount equal to it`() {
        assertThat(AvailableBalance.of(negativeBalance, limit).covers(Money(150))).isTrue()
    }

    @Test
    fun `available balance does not cover an amount above it`() {
        assertThat(AvailableBalance.of(negativeBalance, limit).covers(Money(151))).isFalse()
    }
}
