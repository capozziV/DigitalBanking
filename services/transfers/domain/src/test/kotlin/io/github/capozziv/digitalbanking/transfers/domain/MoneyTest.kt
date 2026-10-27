package io.github.capozziv.digitalbanking.transfers.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class MoneyTest {
    @Test
    fun `money holds a positive amount in cents`() {
        assertThat(Money(1).cents).isEqualTo(1)
    }

    @Test
    fun `money rejects zero`() {
        assertThatThrownBy { Money(0) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `money rejects a negative amount`() {
        assertThatThrownBy { Money(-1) }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
