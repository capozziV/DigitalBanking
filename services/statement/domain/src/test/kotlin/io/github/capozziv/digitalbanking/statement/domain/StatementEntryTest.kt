package io.github.capozziv.digitalbanking.statement.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class StatementEntryTest {
    @Test
    fun `sequence must be at least 1`() {
        assertThatThrownBy { entry(sequence = 0) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `amount must be positive`() {
        assertThatThrownBy { entry(amountCents = 0) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `description longer than 140 characters is rejected`() {
        assertThatThrownBy { entry(description = "x".repeat(141)) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `empty description is rejected`() {
        assertThatThrownBy { entry(description = "") }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `description of 140 characters and null description are accepted`() {
        assertThat(entry(description = "x".repeat(140)).description).hasSize(140)
        assertThat(entry(description = null).description).isNull()
    }

    @Test
    fun `negative balance after the entry is accepted`() {
        assertThat(entry(balanceAfterCents = -5_000).balanceAfterCents).isEqualTo(-5_000)
    }

    @Test
    fun `cursor must point to a sequence of at least 1`() {
        assertThatThrownBy { StatementCursor(0) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThat(StatementCursor(1).beforeSequence).isEqualTo(1)
    }

    private fun entry(
        sequence: Long = 1,
        amountCents: Long = 1_000,
        balanceAfterCents: Long = 1_000,
        description: String? = "deposit",
    ) = StatementEntry(
        entryId = UUID.randomUUID(),
        accountId = UUID.randomUUID(),
        sequence = sequence,
        kind = EntryKind.CREDIT,
        amountCents = amountCents,
        balanceAfterCents = balanceAfterCents,
        description = description,
        createdAt = Instant.parse("2026-11-10T12:00:00Z"),
    )
}
