package io.github.capozziv.digitalbanking.accounts.application.fake

import io.github.capozziv.digitalbanking.accounts.application.AccountBusyException
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class InMemoryAccountRepositoryTest {
    private val account =
        Account(
            UUID.fromString("0199a0b0-0000-7000-8000-0000000000a1"),
            UUID.fromString("0199a0b0-0000-7000-8000-0000000000c1"),
            AccountType.FREE,
        )
    private val accounts = InMemoryAccountRepository().apply { insert(account, "open-key-1") }

    @Test
    fun `lock by id returns the account when it is not busy`() {
        assertThat(accounts.lockById(account.id)).isEqualTo(account)
    }

    @Test
    fun `lock by id throws account busy for an account marked as busy`() {
        accounts.busyAccounts += account.id

        assertThatThrownBy { accounts.lockById(account.id) }
            .isInstanceOfSatisfying(AccountBusyException::class.java) {
                assertThat(it.accountId).isEqualTo(account.id)
            }
    }
}
