package io.github.capozziv.digitalbanking.customer.application.usecase

import io.github.capozziv.digitalbanking.customer.application.CustomerNotFoundByPhoneException
import io.github.capozziv.digitalbanking.customer.application.fake.DirectTransactionRunner
import io.github.capozziv.digitalbanking.customer.application.fake.InMemoryCustomerRepository
import io.github.capozziv.digitalbanking.customer.application.fake.MutableClock
import io.github.capozziv.digitalbanking.customer.application.fake.RecordingEventOutbox
import io.github.capozziv.digitalbanking.customer.domain.PhoneNumber
import io.github.capozziv.digitalbanking.security.Caller
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class FindCustomerByPhoneUseCaseTest {
    private val customers = InMemoryCustomerRepository()
    private val tx = DirectTransactionRunner()
    private val find = FindCustomerByPhoneUseCase(customers, tx)
    private val phone = PhoneNumber("+5511987654321")

    @Test
    fun `finds the customer that has the phone`() {
        val id = UUID.randomUUID()
        RegisterCustomerUseCase(customers, RecordingEventOutbox(), tx, MutableClock(Instant.parse("2026-11-19T10:00:00Z")))
            .execute(RegisterCustomerCommand("Ana Souza", "ana@example.test", phone), Caller.Customer(id))

        val found = find.execute(phone)

        assertThat(found.id).isEqualTo(id)
        assertThat(found.name).isEqualTo("Ana Souza")
        assertThat(tx.readOnlyTransactions).isEqualTo(1)
    }

    @Test
    fun `an unknown phone is not found`() {
        assertThatThrownBy { find.execute(phone) }.isInstanceOf(CustomerNotFoundByPhoneException::class.java)
    }
}
