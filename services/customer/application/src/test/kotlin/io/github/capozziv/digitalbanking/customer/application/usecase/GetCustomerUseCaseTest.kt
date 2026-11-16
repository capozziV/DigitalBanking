package io.github.capozziv.digitalbanking.customer.application.usecase

import io.github.capozziv.digitalbanking.customer.application.CallerHasNoCustomerException
import io.github.capozziv.digitalbanking.customer.application.CustomerNotFoundException
import io.github.capozziv.digitalbanking.customer.application.fake.DirectTransactionRunner
import io.github.capozziv.digitalbanking.customer.application.fake.InMemoryCustomerRepository
import io.github.capozziv.digitalbanking.customer.domain.Customer
import io.github.capozziv.digitalbanking.security.Caller
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class GetCustomerUseCaseTest {
    private val customers = InMemoryCustomerRepository()
    private val tx = DirectTransactionRunner()
    private val getCustomer = GetCustomerUseCase(customers, tx)
    private val getOwnCustomer = GetOwnCustomerUseCase(getCustomer)
    private val id = UUID.randomUUID()

    private fun existingCustomer(): Customer =
        Customer.register(id, "Ana Souza", "ana@example.test", Instant.parse("2026-11-13T10:00:00Z")).first.also {
            customers.insertIfAbsent(it)
        }

    @Test
    fun `finds a customer by id`() {
        val customer = existingCustomer()

        assertThat(getCustomer.execute(id)).isEqualTo(customer)
        assertThat(tx.readOnlyTransactions).isEqualTo(1)
    }

    @Test
    fun `an unknown id is not found`() {
        assertThatThrownBy { getCustomer.execute(id) }.isInstanceOf(CustomerNotFoundException::class.java)
    }

    @Test
    fun `a customer gets its own record`() {
        val customer = existingCustomer()

        assertThat(getOwnCustomer.execute(Caller.Customer(id))).isEqualTo(customer)
    }

    @Test
    fun `an admin gets the customer of its own userId`() {
        val customer = existingCustomer()

        assertThat(getOwnCustomer.execute(Caller.Admin(id))).isEqualTo(customer)
    }

    @Test
    fun `a caller that never registered is not found`() {
        existingCustomer()

        assertThatThrownBy { getOwnCustomer.execute(Caller.Customer(UUID.randomUUID())) }
            .isInstanceOf(CustomerNotFoundException::class.java)
    }

    @Test
    fun `a service caller has no customer of its own`() {
        assertThatThrownBy { getOwnCustomer.execute(Caller.Service) }.isInstanceOf(CallerHasNoCustomerException::class.java)
    }
}
