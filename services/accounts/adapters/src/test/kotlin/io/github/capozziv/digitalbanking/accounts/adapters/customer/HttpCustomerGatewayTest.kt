package io.github.capozziv.digitalbanking.accounts.adapters.customer

import com.sun.net.httpserver.HttpServer
import io.github.capozziv.digitalbanking.accounts.application.CustomersUnavailableException
import io.github.capozziv.digitalbanking.accounts.application.port.CustomerStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.InetSocketAddress
import java.net.http.HttpClient
import java.util.UUID

/** How [HttpCustomerGateway] reads each answer of the `customer` service, against a local HTTP server. */
class HttpCustomerGatewayTest {
    private val customerId = UUID.randomUUID()
    private lateinit var server: HttpServer
    private lateinit var gateway: HttpCustomerGateway

    @Volatile private var answerStatus = 200

    @Volatile private var answerBody = ""

    @Volatile private var requestedPath: String? = null

    @BeforeEach
    fun startServer() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/internal/customers/") { exchange ->
            requestedPath = exchange.requestURI.path
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
        server.stop(0)
    }

    private fun answer(
        status: Int,
        body: String,
    ) {
        answerStatus = status
        answerBody = body
    }

    @Test
    fun `200 with ACTIVE is an active customer, asked at the internal path of that customer`() {
        answer(200, """{"id":"$customerId","status":"ACTIVE"}""")

        assertThat(gateway.findStatus(customerId)).isEqualTo(CustomerStatus.ACTIVE)
        assertThat(requestedPath).isEqualTo("/internal/customers/$customerId")
    }

    @Test
    fun `200 with BLOCKED is a blocked customer`() {
        answer(200, """{"id":"$customerId","status":"BLOCKED"}""")

        assertThat(gateway.findStatus(customerId)).isEqualTo(CustomerStatus.BLOCKED)
    }

    @Test
    fun `404 is no such customer`() {
        answer(404, """{"type":"/problems/customer-not-found","status":404}""")

        assertThat(gateway.findStatus(customerId)).isNull()
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
}
