package io.github.capozziv.digitalbanking.accounts.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class OverdraftLimitTest {
    @Test
    fun `overdraft limit accepts zero`() {
        assertThat(OverdraftLimit(0).cents).isEqualTo(0)
    }

    @Test
    fun `overdraft limit rejects a negative amount`() {
        assertThatThrownBy { OverdraftLimit(-1) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("overdraft limit must not be negative")
    }
}
