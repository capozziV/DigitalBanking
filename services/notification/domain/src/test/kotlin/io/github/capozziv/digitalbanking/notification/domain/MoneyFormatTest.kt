package io.github.capozziv.digitalbanking.notification.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class MoneyFormatTest {
    @Test
    fun `formats zero`() {
        assertThat(MoneyFormat.brl(0)).isEqualTo("R$ 0,00")
    }

    @Test
    fun `pads cents below ten`() {
        assertThat(MoneyFormat.brl(5)).isEqualTo("R$ 0,05")
    }

    @Test
    fun `groups thousands`() {
        assertThat(MoneyFormat.brl(123456)).isEqualTo("R$ 1.234,56")
    }

    @Test
    fun `groups millions`() {
        assertThat(MoneyFormat.brl(100000000)).isEqualTo("R$ 1.000.000,00")
    }

    @Test
    fun `formats the largest long without overflow`() {
        assertThat(MoneyFormat.brl(Long.MAX_VALUE)).isEqualTo("R$ 92.233.720.368.547.758,07")
    }

    @Test
    fun `rejects negative values`() {
        assertThatThrownBy { MoneyFormat.brl(-1) }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
