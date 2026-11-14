package io.github.capozziv.digitalbanking.accounts.bootstrap.web

import io.github.capozziv.digitalbanking.accounts.bootstrap.AccountsIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.net.InetAddress
import java.net.ServerSocket
import java.util.UUID

/** With the `customer` service down (nothing listening on its port), opening an account is 503 and writes nothing. */
class AccountOpeningCustomerDownIntegrationTest : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }

    @Test
    fun `a refused connection to the customer service is 503 customers-unavailable and nothing is written`() {
        val customerId = UUID.randomUUID()

        assertProblem(api.openAccount(customerId), 503, "customers-unavailable")
        assertNothingWrittenFor(customerId)
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun customerServiceDown(registry: DynamicPropertyRegistry) {
            registry.add(AccountsIntegrationTest.CUSTOMER_BASE_URL_OVERRIDE) { "http://127.0.0.1:${closedPort()}" }
        }

        /** A port of the loopback that was free a moment ago, so a connection to it is refused. */
        fun closedPort(): Int = ServerSocket(0, 0, InetAddress.getLoopbackAddress()).use { it.localPort }
    }
}
