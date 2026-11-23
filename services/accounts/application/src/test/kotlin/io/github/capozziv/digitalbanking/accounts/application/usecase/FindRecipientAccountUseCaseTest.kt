package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.NoOpenAccountException
import io.github.capozziv.digitalbanking.accounts.application.fake.DirectTransactionRunner
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountRepository
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountStatus
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class FindRecipientAccountUseCaseTest {
    private val customerId = UUID.fromString("0199a0b0-0000-7000-8000-0000000000c1")
    private val account = Account(UUID.fromString("0199a0b0-0000-7000-8000-0000000000a1"), customerId, AccountType.FREE)
    private val accounts = InMemoryAccountRepository()
    private val useCase = FindRecipientAccountUseCase(accounts, DirectTransactionRunner())

    @Test
    fun `returns the id of the not closed account of the customer`() {
        accounts.insert(account, "open-1")

        assertThat(useCase.execute(customerId)).isEqualTo(account.id)
    }

    @Test
    fun `a blocked account is still found`() {
        accounts.insert(account.copy(status = AccountStatus.BLOCKED), "open-1")

        assertThat(useCase.execute(customerId)).isEqualTo(account.id)
    }

    @Test
    fun `fails with no open account for a closed account`() {
        accounts.insert(account.copy(status = AccountStatus.CLOSED), "open-1")

        assertThatThrownBy { useCase.execute(customerId) }.isInstanceOf(NoOpenAccountException::class.java)
    }

    @Test
    fun `fails with no open account for an unknown customer`() {
        assertThatThrownBy { useCase.execute(UUID.randomUUID()) }.isInstanceOf(NoOpenAccountException::class.java)
    }
}
