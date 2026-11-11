package io.github.capozziv.digitalbanking.accounts.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EntryKindTest {
    @Test
    fun `entry kinds are credit and debit`() {
        assertThat(EntryKind.entries.map { it.name }).containsExactly("CREDIT", "DEBIT")
    }
}
