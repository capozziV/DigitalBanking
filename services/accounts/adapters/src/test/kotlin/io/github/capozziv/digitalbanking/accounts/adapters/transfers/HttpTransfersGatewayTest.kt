package io.github.capozziv.digitalbanking.accounts.adapters.transfers

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.sun.net.httpserver.HttpServer
import io.github.capozziv.digitalbanking.accounts.application.TransfersUnavailableException
import io.github.capozziv.digitalbanking.observability.CorrelationIdRestClientInterceptor
import io.github.capozziv.digitalbanking.security.ServiceTokenClient
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
 * How [HttpTransfersGateway] reads each answer of the `transfers` service. The answers transfers describes in its
 * contracts (a count of N, a count of zero, 401 without a token) come from the stubs generated from them; the ones it
 * does not (a body without a count, a negative count, other statuses, a refused connection) from a local HTTP server.
 */
class HttpTransfersGatewayTest {
    private val accountId = UUID.randomUUID()
    private lateinit var server: HttpServer
    private lateinit var gateway: HttpTransfersGateway

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
        server.createContext("/internal/accounts/") { exchange ->
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
        // A request no stub matched is answered 404 by WireMock, which would pass for a real answer of transfers.
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
    fun `200 with a count is that count, asked with the machine token and the correlation id (contract)`() {
        assertThat(contractGateway().pendingOutgoing(PENDING_ID)).isEqualTo(3)
        contractStubServer!!.verifyThat(
            getRequestedFor(urlEqualTo("/internal/accounts/$PENDING_ID/pending-transfers"))
                .withHeader("Authorization", equalTo("Bearer accounts-test-token"))
                .withHeader("X-Correlation-Id", equalTo("corr-contract")),
        )
    }

    @Test
    fun `200 with count zero is zero (contract)`() {
        assertThat(contractGateway().pendingOutgoing(EMPTY_ID)).isZero()
    }

    @Test
    fun `a call without a token is unavailable, not a count (contract)`() {
        val unauthenticated = contractGateway(withToken = false)

        assertThatThrownBy { unauthenticated.pendingOutgoing(UNAUTHENTICATED_ID) }
            .isInstanceOf(TransfersUnavailableException::class.java)
    }

    /**
     * The gateway as [TransfersClientConfiguration] wires it (machine token and correlation id on every call, or the
     * correlation id alone) against the stubs generated from the contracts of transfers.
     */
    private fun contractGateway(withToken: Boolean = true): HttpTransfersGateway {
        MDC.put("correlationId", "corr-contract")
        val stubUrl = transfersStubs.findStubUrl(TRANSFERS_STUBS_GROUP, TRANSFERS_STUBS_ARTIFACT)
        contractStubServer = WireMock(stubUrl.host, stubUrl.port).also { it.resetRequests() }
        val builder = RestClient.builder().baseUrl(stubUrl.toString()).requestInterceptor(CorrelationIdRestClientInterceptor())
        if (withToken) {
            val tokenClient = ServiceTokenClient(URI.create("http://127.0.0.1:${server.address.port}/token"), "accounts", "secret")
            builder.requestInterceptor(tokenClient.interceptor())
        }
        return HttpTransfersGateway(builder.build())
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

    companion object {
        const val TRANSFERS_STUBS_GROUP = "io.github.capozziv.digitalbanking.transfers"
        const val TRANSFERS_STUBS_ARTIFACT = "bootstrap"

        // The accounts of the contracts (services/transfers/bootstrap/src/contractTest/resources/contracts/http).
        val PENDING_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-0000000000d1")
        val EMPTY_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-0000000000d2")

        // The account whose contract is a call without a token (the stub answers it 401 whatever the headers).
        val UNAUTHENTICATED_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-0000000000d3")

        @JvmField
        @RegisterExtension
        val transfersStubs: StubRunnerExtension =
            StubRunnerExtension()
                .stubsMode(StubsMode.CLASSPATH)
                .downloadStub(TRANSFERS_STUBS_GROUP, TRANSFERS_STUBS_ARTIFACT)
    }
}
