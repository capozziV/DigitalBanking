package io.github.capozziv.digitalbanking.accounts.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class BalanceTest {
    @Test
    fun `zero balance has zero cents`() {
        assertThat(Balance.ZERO).isEqualTo(Balance(0))
    }

    @Test
    fun `adding money increases the balance`() {
        assertThat(Balance(100) + Money(50)).isEqualTo(Balance(150))
    }

    @Test
    fun `subtracting money can make the balance negative`() {
        assertThat(Balance(100) - Money(150)).isEqualTo(Balance(-50))
    }

    @Test
    fun `adding money beyond the maximum balance fails`() {
        assertThatThrownBy { Balance(Long.MAX_VALUE) + Money(1) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("balance overflow")
    }

    @Test
    fun `subtracting money beyond the minimum balance fails`() {
        assertThatThrownBy { Balance(Long.MIN_VALUE) - Money(1) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("balance overflow")
    }

    @Test
    fun `available balance is the balance plus the overdraft limit`() {
        assertThat(AvailableBalance.of(Balance(-50), OverdraftLimit(200)).cents).isEqualTo(150)
    }

    @Test
    fun `available balance beyond the maximum fails instead of wrapping around`() {
        assertThatThrownBy { AvailableBalance.of(Balance(Long.MAX_VALUE), OverdraftLimit(1)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("available balance overflow")
    }

    @Test
    fun `available balance covers an amount equal to it`() {
        assertThat(AvailableBalance.of(Balance(-50), OverdraftLimit(200)).covers(Money(150))).isTrue()
    }

    @Test
    fun `available balance does not cover an amount above it`() {
        assertThat(AvailableBalance.of(Balance(-50), OverdraftLimit(200)).covers(Money(151))).isFalse()
    }
}
