package io.github.capozziv.digitalbanking.accounts.adapters.transfers

import com.sun.net.httpserver.HttpServer
import io.github.capozziv.digitalbanking.accounts.application.TransfersUnavailableException
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

/** How [HttpTransfersGateway] reads each answer of the `transfers` service, against a local HTTP server. */
class HttpTransfersGatewayTest {
    private val accountId = UUID.randomUUID()
    private lateinit var server: HttpServer
    private lateinit var gateway: HttpTransfersGateway

    @Volatile private var answerStatus = 200

    @Volatile private var answerBody = ""

    @Volatile private var requestedPath: String? = null

    @BeforeEach
    fun startServer() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/internal/accounts/") { exchange ->
            requestedPath = exchange.requestURI.path
            val bytes = answerBody.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(answerStatus, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()
        gateway =
            HttpTransfersGateway(
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
    fun `200 with a count is that count, asked at the internal path of the account`() {
        answer(200, """{"count":3}""")

        assertThat(gateway.pendingOutgoing(accountId)).isEqualTo(3)
        assertThat(requestedPath).isEqualTo("/internal/accounts/$accountId/pending-transfers")
    }

    @Test
    fun `200 with count zero is zero`() {
        answer(200, """{"count":0}""")

        assertThat(gateway.pendingOutgoing(accountId)).isZero()
    }

    @Test
    fun `200 without a count is unavailable`() {
        answer(200, """{}""")

        assertUnavailable()
    }

    @Test
    fun `200 with a negative count is unavailable`() {
        answer(200, """{"count":-1}""")

        assertUnavailable()
    }

    @Test
    fun `200 with a body that is not JSON is unavailable`() {
        answer(200, "<html>gateway</html>")

        assertUnavailable()
    }

    @Test
    fun `404, 401, 403 and 500 are unavailable`() {
        for (status in listOf(404, 401, 403, 500)) {
            answer(status, """{"status":$status}""")

            assertUnavailable()
        }
    }

    @Test
    fun `a refused connection is unavailable`() {
        server.stop(0)

        assertUnavailable()
    }

    private fun assertUnavailable() {
        assertThatThrownBy { gateway.pendingOutgoing(accountId) }.isInstanceOf(TransfersUnavailableException::class.java)
    }
}
