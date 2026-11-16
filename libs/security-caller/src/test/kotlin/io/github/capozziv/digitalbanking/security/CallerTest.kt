package io.github.capozziv.digitalbanking.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class CallerTest {
    private val customerId = UUID.fromString("00000000-0000-7000-8000-000000000001")
    private val otherId = UUID.fromString("00000000-0000-7000-8000-000000000002")

    @Test
    fun `a customer acts as itself and not as another customer`() {
        val caller: Caller = Caller.Customer(customerId)

        assertThat(caller.actsAsCustomer(customerId)).isTrue()
        assertThat(caller.actsAsCustomer(otherId)).isFalse()
    }

    @Test
    fun `an admin acts as the customer with its own id only`() {
        val caller: Caller = Caller.Admin(customerId)

        assertThat(caller.actsAsCustomer(customerId)).isTrue()
        assertThat(caller.actsAsCustomer(otherId)).isFalse()
    }

    @Test
    fun `a service never acts as a customer`() {
        assertThat(Caller.Service.actsAsCustomer(customerId)).isFalse()
    }
}
