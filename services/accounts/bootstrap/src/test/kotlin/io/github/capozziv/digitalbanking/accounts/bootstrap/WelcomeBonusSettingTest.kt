package io.github.capozziv.digitalbanking.accounts.bootstrap

import io.github.capozziv.digitalbanking.accounts.domain.Money
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class WelcomeBonusSettingTest {
    @Test
    fun `a positive amount enables the bonus`() {
        assertThat(welcomeBonusOf(10_000)).isEqualTo(Money(10_000))
    }

    @Test
    fun `zero disables the bonus`() {
        assertThat(welcomeBonusOf(0)).isNull()
    }

    @Test
    fun `a negative amount fails at startup instead of silently disabling the bonus`() {
        assertThatThrownBy { welcomeBonusOf(-1) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("accounts.welcome-bonus.amount")
    }
}
