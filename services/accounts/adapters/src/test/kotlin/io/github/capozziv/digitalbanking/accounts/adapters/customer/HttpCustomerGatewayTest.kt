package io.github.capozziv.digitalbanking.accounts.adapters.customer

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.sun.net.httpserver.HttpServer
import io.github.capozziv.digitalbanking.accounts.application.CustomersUnavailableException
import io.github.capozziv.digitalbanking.accounts.application.port.CustomerStatus
import io.github.capozziv.digitalbanking.security.ServiceTokenClient
import io.micrometer.observation.ObservationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.slf4j.MDC
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import sh.stubborn.contract.stubrunner.StubsMode
import sh.stubborn.contract.stubrunner.junit.StubRunnerExtension
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.util.UUID

/**
 * How [HttpCustomerGateway] reads each answer of the `customer` service. The answers customer describes in its contracts
 * (ACTIVE, BLOCKED, customer-not-found) come from the stubs generated from them; the ones it does not (another
 * customer, unknown status, stray bodies, 401/403/500, a refused connection) from a local HTTP server, which also
 * plays the token endpoint.
 */
class HttpCustomerGatewayTest {
    private val customerId = UUID.randomUUID()
    private lateinit var server: HttpServer
    private lateinit var gateway: HttpCustomerGateway

    /** Admin client of the stub server of the contracts, once a test asked for it. */
    private var contractStubServer: WireMock? = null

    @Volatile private var answerStatus = 200

    @Volatile private var answerBody = ""

    @BeforeEach
    fun startServer() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/token") { exchange ->
            exchange.requestBody.readAllBytes()
            val token = """{"access_token":"accounts-test-token","token_type":"Bearer","expires_in":300}""".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, token.size.toLong())
            exchange.responseBody.use { it.write(token) }
        }
        server.createContext("/internal/customers/") { exchange ->
            val bytes = answerBody.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(answerStatus, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()
        gateway =
            HttpCustomerGateway(
                RestClient
                    .builder()
                    .baseUrl("http://127.0.0.1:${server.address.port}")
                    .requestFactory(JdkClientHttpRequestFactory(httpClient))
                    .build(),
            )
    }

    @AfterEach
    fun stopServer() {
        // A request no stub matched is answered 404 by WireMock, which would pass for a real customer-not-found.
        val unmatched = contractStubServer?.findAllUnmatchedRequests().orEmpty()
        MDC.clear()
        server.stop(0)
        assertThat(unmatched).describedAs("requests that matched no contract").isEmpty()
    }

    private fun answer(
        status: Int,
        body: String,
    ) {
        answerStatus = status
        answerBody = body
    }

    @Test
    fun `an active customer is ACTIVE, asked with the machine token and the correlation id (contract)`() {
        assertThat(contractGateway().findStatus(ACTIVE_ID)).isEqualTo(CustomerStatus.ACTIVE)
        contractStubServer!!.verifyThat(
            getRequestedFor(urlEqualTo("/internal/customers/$ACTIVE_ID"))
                .withHeader("Authorization", equalTo("Bearer accounts-test-token"))
                .withHeader("X-Correlation-Id", equalTo("corr-contract")),
        )
    }

    @Test
    fun `a blocked customer is BLOCKED (contract)`() {
        assertThat(contractGateway().findStatus(BLOCKED_ID)).isEqualTo(CustomerStatus.BLOCKED)
    }

    @Test
    fun `404 customer-not-found is no such customer (contract)`() {
        assertThat(contractGateway().findStatus(MISSING_ID)).isNull()
    }

    /**
     * The gateway as [CustomerClientConfiguration] wires it (machine token and correlation id on every call) against
     * the stubs generated from the contracts of customer, which answer only a call with both.
     */
    private fun contractGateway(): HttpCustomerGateway {
        MDC.put("correlationId", "corr-contract")
        val stubUrl = customerStubs.findStubUrl(CUSTOMER_STUBS_GROUP, CUSTOMER_STUBS_ARTIFACT)
        contractStubServer = WireMock(stubUrl.host, stubUrl.port).also { it.resetRequests() }
        val url = stubUrl.toString()
        val tokenClient = ServiceTokenClient(URI.create("http://127.0.0.1:${server.address.port}/token"), "accounts", "secret")
        val configuration = CustomerClientConfiguration()
        return HttpCustomerGateway(
            configuration.customerRestClient(CustomerClientProperties(baseUrl = url), tokenClient, ObservationRegistry.NOOP),
        )
    }

    @Test
    fun `200 for another customer is unavailable, not that customer's status`() {
        answer(200, """{"id":"${UUID.randomUUID()}","status":"ACTIVE"}""")

        assertUnavailable()
    }

    @Test
    fun `200 with an unknown status is unavailable`() {
        answer(200, """{"id":"$customerId","status":"SUSPENDED"}""")

        assertUnavailable()
    }

    @Test
    fun `200 without a status is unavailable`() {
        answer(200, """{"id":"$customerId"}""")

        assertUnavailable()
    }

    @Test
    fun `200 with a body that is not JSON is unavailable`() {
        answer(200, "<html>gateway</html>")

        assertUnavailable()
    }

    @Test
    fun `401 is unavailable, not a missing customer`() {
        answer(401, """{"status":401}""")

        assertUnavailable()
    }

    @Test
    fun `403 is unavailable, not a missing customer`() {
        answer(403, """{"status":403}""")

        assertUnavailable()
    }

    @Test
    fun `500 is unavailable`() {
        answer(500, """{"status":500}""")

        assertUnavailable()
    }

    @Test
    fun `a refused connection is unavailable`() {
        server.stop(0)

        assertUnavailable()
    }

    private fun assertUnavailable() {
        assertThatThrownBy { gateway.findStatus(customerId) }.isInstanceOf(CustomersUnavailableException::class.java)
    }

    companion object {
        const val CUSTOMER_STUBS_GROUP = "io.github.capozziv.digitalbanking.customer"
        const val CUSTOMER_STUBS_ARTIFACT = "bootstrap"

        // The customers of the contracts (services/customer/bootstrap/src/contractTest/resources/contracts/http).
        val ACTIVE_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-0000000000c1")
        val BLOCKED_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-0000000000b1")
        val MISSING_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-000000000404")

        @JvmField
        @RegisterExtension
        val customerStubs: StubRunnerExtension =
            StubRunnerExtension()
                .stubsMode(StubsMode.CLASSPATH)
                .downloadStub(CUSTOMER_STUBS_GROUP, CUSTOMER_STUBS_ARTIFACT)
    }
}
