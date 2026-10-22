package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.CustomerAlreadyHasAccountException
import io.github.capozziv.digitalbanking.accounts.application.fake.DirectTransactionRunner
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountRepository
import io.github.capozziv.digitalbanking.accounts.application.fake.RecordingEventOutbox
import io.github.capozziv.digitalbanking.accounts.application.fake.SequentialIdGenerator
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountOpened
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class OpenAccountUseCaseTest {
    private val customerId = UUID.fromString("0199a0b0-0000-7000-8000-0000000000c1")
    private val accounts = InMemoryAccountRepository()
    private val outbox = RecordingEventOutbox()
    private val tx = DirectTransactionRunner()
    private val useCase = OpenAccountUseCase(accounts, outbox, tx, SequentialIdGenerator())

    private val expectedAccount = Account(SequentialIdGenerator.idAt(1), customerId, AccountType.FREE)

    @Test
    fun `opening a new account creates a free account and appends account opened to the outbox`() {
        val result = useCase.execute(OpenAccountCommand(customerId, "open-key-1"))

        assertThat(result).isEqualTo(OpenAccountResult(expectedAccount, created = true))
        assertThat(accounts.all()).containsExactly(expectedAccount)
        assertThat(outbox.events).containsExactly(AccountOpened(expectedAccount.id, customerId, AccountType.FREE))
        assertThat(tx.transactions).isEqualTo(1)
    }

    @Test
    fun `retrying with the same opening key returns the same account without a new event`() {
        useCase.execute(OpenAccountCommand(customerId, "open-key-1"))

        val retry = useCase.execute(OpenAccountCommand(customerId, "open-key-1"))

        assertThat(retry).isEqualTo(OpenAccountResult(expectedAccount, created = false))
        assertThat(accounts.all()).containsExactly(expectedAccount)
        assertThat(outbox.events).containsExactly(AccountOpened(expectedAccount.id, customerId, AccountType.FREE))
    }

    @Test
    fun `opening a second account for the same customer with another key fails`() {
        useCase.execute(OpenAccountCommand(customerId, "open-key-1"))

        assertThatThrownBy { useCase.execute(OpenAccountCommand(customerId, "open-key-2")) }
            .isInstanceOfSatisfying(CustomerAlreadyHasAccountException::class.java) {
                assertThat(it.customerId).isEqualTo(customerId)
            }
        assertThat(accounts.all()).containsExactly(expectedAccount)
        assertThat(outbox.events).containsExactly(AccountOpened(expectedAccount.id, customerId, AccountType.FREE))
    }
}
