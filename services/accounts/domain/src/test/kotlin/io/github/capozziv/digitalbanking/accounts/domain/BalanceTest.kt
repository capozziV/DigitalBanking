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
            .isInstanceOf(BalanceOverflowException::class.java)
            .hasMessage("balance overflow")
    }

    @Test
    fun `subtracting money beyond the minimum balance fails`() {
        assertThatThrownBy { Balance(Long.MIN_VALUE) - Money(1) }
            .isInstanceOf(BalanceOverflowException::class.java)
            .hasMessage("balance overflow")
    }
}
