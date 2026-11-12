package io.github.capozziv.digitalbanking.accounts.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AccountTypeTest {
    @Test
    fun `account types follow the glossary`() {
        assertThat(AccountType.entries.map { it.name })
            .containsExactly("FREE", "STANDARD", "GOLD", "BLACK", "DIAMOND")
    }
}
