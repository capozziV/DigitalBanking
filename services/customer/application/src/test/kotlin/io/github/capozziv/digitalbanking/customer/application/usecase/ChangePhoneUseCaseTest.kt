package io.github.capozziv.digitalbanking.customer.application.usecase

import io.github.capozziv.digitalbanking.customer.application.CallerHasNoCustomerException
import io.github.capozziv.digitalbanking.customer.application.CustomerNotFoundException
import io.github.capozziv.digitalbanking.customer.application.PhoneAlreadyInUseException
import io.github.capozziv.digitalbanking.customer.application.fake.DirectTransactionRunner
import io.github.capozziv.digitalbanking.customer.application.fake.InMemoryCustomerRepository
import io.github.capozziv.digitalbanking.customer.application.fake.MutableClock
import io.github.capozziv.digitalbanking.customer.application.fake.RecordingEventOutbox
import io.github.capozziv.digitalbanking.customer.domain.CustomerPhoneChanged
import io.github.capozziv.digitalbanking.customer.domain.PhoneNumber
import io.github.capozziv.digitalbanking.security.Caller
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ChangePhoneUseCaseTest {
    private val customers = InMemoryCustomerRepository()
    private val outbox = RecordingEventOutbox()
    private val tx = DirectTransactionRunner()
    private val clock = MutableClock(Instant.parse("2026-11-19T10:00:00Z"))
    private val register = RegisterCustomerUseCase(customers, outbox, tx, clock)
    private val changePhone = ChangePhoneUseCase(customers, outbox, tx, clock)
    private val sub = UUID.randomUUID()
    private val phone = PhoneNumber("+5511987654321")
    private val newPhone = PhoneNumber("+5521988776655")

    private fun registered(
        id: UUID = sub,
        number: PhoneNumber = phone,
    ) = register.execute(RegisterCustomerCommand("Ana Souza", "ana@example.test", number), Caller.Customer(id))

    @Test
    fun `the customer changes its phone and CustomerPhoneChanged goes to the outbox`() {
        registered()
        clock.advance(60)

        val changed = changePhone.execute(newPhone, Caller.Customer(sub))

        assertThat(changed.phone).isEqualTo(newPhone)
        assertThat(changed.updatedAt).isEqualTo(clock.instant())
        assertThat(customers.findById(sub)).isEqualTo(changed)
        assertThat(outbox.events.last()).isEqualTo(CustomerPhoneChanged(sub, phone, newPhone))
        assertThat(customers.locks).isEqualTo(1)
    }

    @Test
    fun `the same phone again changes nothing and writes no event`() {
        val first = registered().customer
        clock.advance(60)

        val same = changePhone.execute(phone, Caller.Customer(sub))

        assertThat(same).isEqualTo(first)
        assertThat(outbox.events).hasSize(1)
    }

    @Test
    fun `a phone another customer has is refused and the phone stays`() {
        registered()
        registered(UUID.randomUUID(), newPhone)

        assertThatThrownBy { changePhone.execute(newPhone, Caller.Customer(sub)) }.isInstanceOf(PhoneAlreadyInUseException::class.java)

        assertThat(customers.findById(sub)!!.phone).isEqualTo(phone)
        assertThat(outbox.events).hasSize(2)
    }

    @Test
    fun `an unregistered caller has no customer`() {
        assertThatThrownBy { changePhone.execute(newPhone, Caller.Customer(sub)) }.isInstanceOf(CustomerNotFoundException::class.java)
    }

    @Test
    fun `a service caller has no customer to change`() {
        assertThatThrownBy { changePhone.execute(newPhone, Caller.Service) }.isInstanceOf(CallerHasNoCustomerException::class.java)
    }
}
