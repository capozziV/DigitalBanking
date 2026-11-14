package io.github.capozziv.digitalbanking.customer.application.usecase

import io.github.capozziv.digitalbanking.customer.application.CallerHasNoCustomerException
import io.github.capozziv.digitalbanking.customer.application.fake.DirectTransactionRunner
import io.github.capozziv.digitalbanking.customer.application.fake.InMemoryCustomerRepository
import io.github.capozziv.digitalbanking.customer.application.fake.MutableClock
import io.github.capozziv.digitalbanking.customer.application.fake.RecordingEventOutbox
import io.github.capozziv.digitalbanking.customer.domain.CustomerRegistered
import io.github.capozziv.digitalbanking.customer.domain.CustomerStatus
import io.github.capozziv.digitalbanking.security.Caller
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RegisterCustomerUseCaseTest {
    private val customers = InMemoryCustomerRepository()
    private val outbox = RecordingEventOutbox()
    private val tx = DirectTransactionRunner()
    private val clock = MutableClock(Instant.parse("2026-11-13T10:00:00Z"))
    private val register = RegisterCustomerUseCase(customers, outbox, tx, clock)
    private val sub = UUID.randomUUID()
    private val command = RegisterCustomerCommand("Ana Souza", "ana@example.test")

    @Test
    fun `a customer registers itself with its sub as id, active, and CustomerRegistered goes to the outbox`() {
        val result = register.execute(command, Caller.Customer(sub))

        assertThat(result.created).isTrue()
        assertThat(result.customer.id).isEqualTo(sub)
        assertThat(result.customer.status).isEqualTo(CustomerStatus.ACTIVE)
        assertThat(result.customer.createdAt).isEqualTo(clock.instant())
        assertThat(customers.all()).containsExactly(result.customer)
        assertThat(outbox.events).containsExactly(CustomerRegistered(sub, "Ana Souza", "ana@example.test"))
        assertThat(tx.transactions).isEqualTo(1)
    }

    @Test
    fun `an admin registers its own customer, with its userId as id`() {
        val result = register.execute(command, Caller.Admin(sub))

        assertThat(result.created).isTrue()
        assertThat(result.customer.id).isEqualTo(sub)
    }

    @Test
    fun `registering again returns the same customer, created is false and no second event is written`() {
        val first = register.execute(command, Caller.Customer(sub))
        clock.advance(60)

        val second = register.execute(RegisterCustomerCommand("Other Name", "other@example.test"), Caller.Customer(sub))

        assertThat(second.created).isFalse()
        assertThat(second.customer).isEqualTo(first.customer)
        assertThat(customers.all()).hasSize(1)
        assertThat(outbox.events).hasSize(1)
    }

    @Test
    fun `a service caller has no customer to register`() {
        assertThatThrownBy { register.execute(command, Caller.Service) }.isInstanceOf(CallerHasNoCustomerException::class.java)

        assertThat(customers.all()).isEmpty()
        assertThat(outbox.events).isEmpty()
    }
}
