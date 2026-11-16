package io.github.capozziv.digitalbanking.customer.application.usecase

import io.github.capozziv.digitalbanking.customer.application.CustomerNotFoundException
import io.github.capozziv.digitalbanking.customer.application.fake.DirectTransactionRunner
import io.github.capozziv.digitalbanking.customer.application.fake.InMemoryCustomerRepository
import io.github.capozziv.digitalbanking.customer.application.fake.MutableClock
import io.github.capozziv.digitalbanking.customer.application.fake.RecordingEventOutbox
import io.github.capozziv.digitalbanking.customer.domain.Customer
import io.github.capozziv.digitalbanking.customer.domain.CustomerBlocked
import io.github.capozziv.digitalbanking.customer.domain.CustomerStatus
import io.github.capozziv.digitalbanking.customer.domain.CustomerUnblocked
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ChangeCustomerStatusUseCaseTest {
    private val customers = InMemoryCustomerRepository()
    private val outbox = RecordingEventOutbox()
    private val tx = DirectTransactionRunner()
    private val clock = MutableClock(Instant.parse("2026-11-13T10:00:00Z"))
    private val changeStatus = ChangeCustomerStatusUseCase(customers, outbox, tx, clock)
    private val id = UUID.randomUUID()

    private fun existingCustomer(): Customer =
        Customer.register(id, "Ana Souza", "ana@example.test", clock.instant()).first.also { customers.insertIfAbsent(it) }

    @Test
    fun `blocking writes the new status and CustomerBlocked in one transaction`() {
        existingCustomer()
        clock.advance(60)

        val result = changeStatus.execute(id, CustomerStatus.BLOCKED)

        assertThat(result.status).isEqualTo(CustomerStatus.BLOCKED)
        assertThat(result.updatedAt).isEqualTo(clock.instant())
        assertThat(customers.findById(id)).isEqualTo(result)
        assertThat(outbox.events).containsExactly(CustomerBlocked(id))
        assertThat(tx.transactions).isEqualTo(1)
        assertThat(customers.locks).describedAs("the row is locked while it changes").isEqualTo(1)
    }

    @Test
    fun `unblocking writes CustomerUnblocked`() {
        existingCustomer()
        changeStatus.execute(id, CustomerStatus.BLOCKED)

        val result = changeStatus.execute(id, CustomerStatus.ACTIVE)

        assertThat(result.status).isEqualTo(CustomerStatus.ACTIVE)
        assertThat(outbox.events).containsExactly(CustomerBlocked(id), CustomerUnblocked(id))
    }

    @Test
    fun `setting the current status again writes no event and keeps the updated time`() {
        val customer = existingCustomer()
        clock.advance(60)

        val result = changeStatus.execute(id, CustomerStatus.ACTIVE)

        assertThat(result).isEqualTo(customer)
        assertThat(outbox.events).isEmpty()
    }

    @Test
    fun `an unknown customer is not found and nothing is written`() {
        assertThatThrownBy { changeStatus.execute(id, CustomerStatus.BLOCKED) }.isInstanceOf(CustomerNotFoundException::class.java)

        assertThat(outbox.events).isEmpty()
    }
}
