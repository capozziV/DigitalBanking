package io.github.capozziv.digitalbanking.transfers.bootstrap.customer

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.http.Fault
import io.github.capozziv.digitalbanking.transfers.adapters.customer.CustomerClientConfiguration
import io.github.capozziv.digitalbanking.transfers.adapters.customer.CustomerClientProperties
import io.github.capozziv.digitalbanking.transfers.application.port.CustomerGateway
import io.github.capozziv.digitalbanking.transfers.application.port.CustomerLookup
import io.github.capozziv.digitalbanking.transfers.bootstrap.ServiceTokenServers
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import java.time.Duration
import java.util.UUID

/** The customer client against a WireMock that answers 401 to any request without the service token. */
class HttpCustomerGatewayIntegrationTest {
    private val server = WireMockServer(ServiceTokenServers.peerOptions())
    private val tokenServer = WireMockServer(options().dynamicPort())
    private lateinit var gateway: CustomerGateway

    private val customerId = UUID.fromString("0198a1b2-0000-7000-8000-0000000000c1")
    private val path = "/internal/customers/$customerId"

    @BeforeEach
    fun startServers() {
        server.start()
        tokenServer.start()
        ServiceTokenServers.stubTokenEndpoint(tokenServer)
        val properties = CustomerClientProperties(url = "http://localhost:${server.port()}", timeout = Duration.ofMillis(500))
        gateway = CustomerClientConfiguration().customerGateway(properties, ServiceTokenServers.clientOf(tokenServer))
    }

    @AfterEach
    fun stopServers() {
        MDC.clear()
        server.stop()
        tokenServer.stop()
    }

    @Test
    fun `an active customer is ACTIVE, asked with the service token and the correlation id`() {
        server.stubFor(get(urlEqualTo(path)).willReturn(customer("ACTIVE")))
        MDC.put("correlationId", "corr-c-1")

        assertThat(gateway.lookupCustomer(customerId)).isEqualTo(CustomerLookup.ACTIVE)
        server.verify(
            getRequestedFor(urlEqualTo(path))
                .withHeader("Authorization", equalTo("Bearer ${ServiceTokenServers.SERVICE_TOKEN}"))
                .withHeader("X-Correlation-Id", equalTo("corr-c-1")),
        )
    }

    @Test
    fun `a blocked customer is BLOCKED`() {
        server.stubFor(get(urlEqualTo(path)).willReturn(customer("BLOCKED")))

        assertThat(gateway.lookupCustomer(customerId)).isEqualTo(CustomerLookup.BLOCKED)
    }

    @Test
    fun `404 is NOT_FOUND`() {
        server.stubFor(
            get(urlEqualTo(path)).willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/problem+json")
                    .withBody("""{"type":"/problems/customer-not-found","status":404}"""),
            ),
        )

        assertThat(gateway.lookupCustomer(customerId)).isEqualTo(CustomerLookup.NOT_FOUND)
    }

    @Test
    fun `5xx, a timeout, a cut connection, an unknown status or a body that is not a customer are UNAVAILABLE`() {
        val unavailable =
            listOf(
                aResponse().withStatus(500),
                aResponse().withStatus(503),
                customer("ACTIVE").withFixedDelay(1_500),
                aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER),
                customer("SUSPENDED"),
                aResponse().withStatus(200).withBody("<html></html>"),
                aResponse().withStatus(403),
                // A 404 that is not customer-not-found (a wrong base URL, a proxy) is not a missing customer.
                aResponse().withStatus(404),
                aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/problem+json")
                    .withBody("""{"type":"/problems/route-not-found","status":404}"""),
            )
        for (response in unavailable) {
            server.stubFor(get(urlEqualTo(path)).willReturn(response))

            assertThat(gateway.lookupCustomer(customerId)).isEqualTo(CustomerLookup.UNAVAILABLE)
        }
    }

    @Test
    fun `no service token is UNAVAILABLE and customer is not called`() {
        tokenServer.stop()

        assertThat(gateway.lookupCustomer(customerId)).isEqualTo(CustomerLookup.UNAVAILABLE)
        assertThat(server.allServeEvents).isEmpty()
    }

    private fun customer(status: String): ResponseDefinitionBuilder =
        aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("""{"id":"$customerId","status":"$status"}""")
}
