package io.github.capozziv.digitalbanking.statement.application.usecase

import io.github.capozziv.digitalbanking.statement.application.fake.InMemoryStatementRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class RegisterAccountUseCaseTest {
    private val repository = InMemoryStatementRepository()
    private val useCase = RegisterAccountUseCase(repository)

    @Test
    fun `registering the same account twice is not an error`() {
        val account = UUID.randomUUID()
        val customer = UUID.randomUUID()

        useCase.execute(account, customer)
        useCase.execute(account, customer)

        assertThat(repository.accountExists(account)).isTrue()
    }
}
