package io.github.capozziv.digitalbanking.accounts.domain

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

    @Test
    fun `overdraft limit accepts zero`() {
        assertThat(OverdraftLimit(0).cents).isEqualTo(0)
    }

    @Test
    fun `overdraft limit rejects a negative amount`() {
        assertThatThrownBy { OverdraftLimit(-1) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `account types follow the glossary`() {
        assertThat(AccountType.entries.map { it.name })
            .containsExactly("FREE", "STANDARD", "GOLD", "BLACK", "DIAMOND")
    }

    @Test
    fun `entry kinds are credit and debit`() {
        assertThat(EntryKind.entries.map { it.name }).containsExactly("CREDIT", "DEBIT")
    }
}
