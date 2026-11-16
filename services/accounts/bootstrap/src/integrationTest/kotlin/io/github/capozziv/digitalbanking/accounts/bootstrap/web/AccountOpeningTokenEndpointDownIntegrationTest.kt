package io.github.capozziv.digitalbanking.accounts.bootstrap.web

import io.github.capozziv.digitalbanking.accounts.bootstrap.AccountsIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.UUID

/**
 * With the token endpoint down, no machine token can be had to ask the `customer` service: opening an account is 503,
 * the `customer` service is never asked and nothing is written.
 */
class AccountOpeningTokenEndpointDownIntegrationTest : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }

    @Test
    fun `no machine token is 503 customers-unavailable, without asking the customer service, and nothing is written`() {
        val customerId = UUID.randomUUID()

        assertProblem(api.openAccount(customerId), 503, "customers-unavailable")
        assertThat(customerService.lookupsOf(customerId)).isEmpty()
        assertNothingWrittenFor(customerId)
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun tokenEndpointDown(registry: DynamicPropertyRegistry) {
            val closedPort = AccountOpeningCustomerDownIntegrationTest.closedPort()
            registry.add(AccountsIntegrationTest.TOKEN_URI_OVERRIDE) { "http://127.0.0.1:$closedPort/token" }
        }
    }
}
