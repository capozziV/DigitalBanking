package io.github.capozziv.digitalbanking.customer.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class CustomerTest {
    private val id = UUID.fromString("00000000-0000-7000-8000-000000000001")
    private val now = Instant.parse("2026-11-13T10:00:00Z")
    private val later = now.plusSeconds(60)

    @Test
    fun `register creates an active customer and the CustomerRegistered event`() {
        val (customer, event) = Customer.register(id, "Ana Souza", "ana@example.test", now)

        assertThat(customer).isEqualTo(Customer(id, "Ana Souza", "ana@example.test", CustomerStatus.ACTIVE, now, now))
        assertThat(event).isEqualTo(CustomerRegistered(id, "Ana Souza", "ana@example.test"))
    }

    @Test
    fun `register trims the name and the email`() {
        val (customer, _) = Customer.register(id, "  Ana Souza ", " ana@example.test  ", now)

        assertThat(customer.name).isEqualTo("Ana Souza")
        assertThat(customer.email).isEqualTo("ana@example.test")
    }

    @Test
    fun `a blank or too long name is refused`() {
        assertThatThrownBy { Customer.register(id, "   ", "ana@example.test", now) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy {
            Customer.register(id, "a".repeat(Customer.MAX_NAME_LENGTH + 1), "ana@example.test", now)
        }.isInstanceOf(IllegalArgumentException::class.java)
        assertThat(Customer.register(id, "a".repeat(Customer.MAX_NAME_LENGTH), "ana@example.test", now).first.name)
            .hasSize(Customer.MAX_NAME_LENGTH)
    }

    @Test
    fun `an email without an at sign, a domain or with spaces is refused`() {
        listOf("", "ana", "ana@", "@example.test", "ana@example", "a na@example.test", "ana@@example.test").forEach {
            assertThatThrownBy { Customer.register(id, "Ana", it, now) }
                .describedAs(it)
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun `an email over the maximum length is refused`() {
        val tooLong = "a".repeat(Customer.MAX_EMAIL_LENGTH) + "@example.test"

        assertThatThrownBy { Customer.register(id, "Ana", tooLong, now) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `blocking an active customer sets BLOCKED, updates the time and gives CustomerBlocked`() {
        val customer = Customer.register(id, "Ana", "ana@example.test", now).first

        val (blocked, event) = customer.changeStatus(CustomerStatus.BLOCKED, later)

        assertThat(blocked).isEqualTo(customer.copy(status = CustomerStatus.BLOCKED, updatedAt = later))
        assertThat(blocked.createdAt).isEqualTo(now)
        assertThat(event).isEqualTo(CustomerBlocked(id))
    }

    @Test
    fun `unblocking a blocked customer gives CustomerUnblocked`() {
        val blocked =
            Customer
                .register(id, "Ana", "ana@example.test", now)
                .first
                .changeStatus(CustomerStatus.BLOCKED, later)
                .first

        val (unblocked, event) = blocked.changeStatus(CustomerStatus.ACTIVE, later.plusSeconds(1))

        assertThat(unblocked.status).isEqualTo(CustomerStatus.ACTIVE)
        assertThat(event).isEqualTo(CustomerUnblocked(id))
    }

    @Test
    fun `setting the status the customer already has changes nothing and gives no event`() {
        val customer = Customer.register(id, "Ana", "ana@example.test", now).first

        val (same, event) = customer.changeStatus(CustomerStatus.ACTIVE, later)

        assertThat(same).isEqualTo(customer)
        assertThat(event).isNull()
    }
}
