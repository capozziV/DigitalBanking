package io.github.capozziv.digitalbanking.accounts.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class LedgerEntryTest {
    private fun entry(
        sequence: Long = 1,
        description: String? = null,
        balanceAfter: Long = 100,
    ) = LedgerEntry(
        id = UUID.fromString("0199a0b0-0000-7000-8000-000000000003"),
        accountId = UUID.fromString("0199a0b0-0000-7000-8000-000000000001"),
        sequence = sequence,
        kind = EntryKind.CREDIT,
        amount = Money(100),
        balanceAfter = Balance(balanceAfter),
        description = description,
        idempotencyKey = "key-1",
        createdAt = Instant.parse("2026-10-22T12:00:00Z"),
    )

    @Test
    fun `description with 140 characters is accepted`() {
        assertThat(entry(description = "a".repeat(140)).description).hasSize(140)
    }

    @Test
    fun `description with 141 characters is rejected`() {
        assertThatThrownBy { entry(description = "a".repeat(141)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("description must have 1 to 140 characters")
    }

    @Test
    fun `empty description is rejected`() {
        assertThatThrownBy { entry(description = "") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("description must have 1 to 140 characters")
    }

    @Test
    fun `null description is accepted`() {
        assertThat(entry(description = null).description).isNull()
    }

    @Test
    fun `sequence below 1 is rejected`() {
        assertThatThrownBy { entry(sequence = 0) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("sequence must be at least 1")
    }

    @Test
    fun `without a last entry the current balance is zero and the next sequence is 1`() {
        val last: LedgerEntry? = null

        assertThat(last.currentBalance()).isEqualTo(Balance.ZERO)
        assertThat(last.nextSequence()).isEqualTo(1)
    }

    @Test
    fun `with a last entry the current balance is its balance after and the next sequence follows it`() {
        val last: LedgerEntry? = entry(sequence = 5, balanceAfter = -250)

        assertThat(last.currentBalance()).isEqualTo(Balance(-250))
        assertThat(last.nextSequence()).isEqualTo(6)
    }
}
