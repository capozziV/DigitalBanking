package io.github.capozziv.digitalbanking.transfers.bootstrap.accounts

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.http.Fault
import io.github.capozziv.digitalbanking.transfers.adapters.accounts.AccountsClientConfiguration
import io.github.capozziv.digitalbanking.transfers.adapters.accounts.AccountsClientProperties
import io.github.capozziv.digitalbanking.transfers.adapters.accounts.AccountsResponseClassifier
import io.github.capozziv.digitalbanking.transfers.adapters.accounts.HttpAccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.port.AccountLookup
import io.github.capozziv.digitalbanking.transfers.application.port.AccountsGateway
import io.github.capozziv.digitalbanking.transfers.domain.AccountsCall
import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.StepOutcome
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import java.net.ServerSocket
import java.time.Duration
import java.util.UUID
import kotlin.concurrent.thread

class HttpAccountsGatewayIntegrationTest {
    private val server = WireMockServer(options().dynamicPort())
    private lateinit var gateway: AccountsGateway

    private val accountId = UUID.fromString("0198a1b2-0000-7000-8000-000000000001")
    private val entryId = "0198a1b2-0000-7000-8000-0000000000aa"

    @BeforeEach
    fun startServer() {
        server.start()
        gateway = gatewayFor("http://localhost:${server.port()}")
    }

    @AfterEach
    fun stopServer() {
        MDC.clear()
        server.stop()
    }

    private fun gatewayFor(url: String): AccountsGateway {
        val properties = AccountsClientProperties(url = url)
        return HttpAccountsGateway(AccountsClientConfiguration().accountsRestClient(properties), AccountsResponseClassifier())
    }

    private fun debit(key: String = "transfer-1-debit") = AccountsCall.Debit(accountId, Money(1_000), key, "Transfer to someone")

    private fun entryJson() =
        """{"id":"$entryId","accountId":"$accountId","sequence":1,"kind":"DEBIT","amount":1000,"balanceAfter":0,""" +
            """"description":"Transfer to someone","createdAt":"2026-10-27T10:00:00Z"}"""

    private fun problem(
        status: Int,
        slug: String,
    ) = aResponse()
        .withStatus(status)
        .withHeader("Content-Type", "application/problem+json")
        .withBody("""{"type":"/problems/$slug","title":"t","status":$status}""")

    @Test
    fun `debit sends the key, the correlation id and the body, and 201 is success`() {
        server.stubFor(
            post(urlEqualTo("/accounts/$accountId/debits"))
                .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json").withBody(entryJson())),
        )
        MDC.put("correlationId", "corr-123")

        val result = gateway.perform(debit())

        assertThat(result.outcome).isEqualTo(StepOutcome.Success)
        assertThat(result.httpStatus).isEqualTo(201)
        server.verify(
            postRequestedFor(urlEqualTo("/accounts/$accountId/debits"))
                .withHeader("Idempotency-Key", equalTo("transfer-1-debit"))
                .withHeader("X-Correlation-Id", equalTo("corr-123"))
                .withHeader(
                    "Content-Type",
                    com.github.tomakehurst.wiremock.client.WireMock
                        .containing("application/json"),
                ).withRequestBody(equalToJson("""{"amount":1000,"description":"Transfer to someone"}""", false, false)),
        )
    }

    @Test
    fun `credit goes to the credits path and 200 (repetition) is success`() {
        server.stubFor(
            post(urlEqualTo("/accounts/$accountId/credits"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(entryJson())),
        )

        val result = gateway.perform(AccountsCall.Credit(accountId, Money(1_000), "transfer-1-credit", "Transfer from x"))

        assertThat(result.outcome).isEqualTo(StepOutcome.Success)
        assertThat(result.httpStatus).isEqualTo(200)
        server.verify(
            postRequestedFor(urlEqualTo("/accounts/$accountId/credits"))
                .withHeader("Idempotency-Key", equalTo("transfer-1-credit")),
        )
    }

    @Test
    fun `a call without correlation id in the MDC sends no X-Correlation-Id header`() {
        server.stubFor(
            post(urlEqualTo("/accounts/$accountId/debits")).willReturn(aResponse().withStatus(201).withBody(entryJson())),
        )

        gateway.perform(debit())

        server.verify(
            postRequestedFor(urlEqualTo("/accounts/$accountId/debits"))
                .withoutHeader("X-Correlation-Id"),
        )
    }

    @Test
    fun `422 insufficient-available-balance is a refusal`() {
        server.stubFor(
            post(urlEqualTo("/accounts/$accountId/debits"))
                .willReturn(problem(422, "insufficient-available-balance")),
        )

        val result = gateway.perform(debit())

        assertThat(result.outcome).isEqualTo(StepOutcome.Refused(StepOutcome.INSUFFICIENT_AVAILABLE_BALANCE))
        assertThat(result.httpStatus).isEqualTo(422)
    }

    @Test
    fun `404 account-not-found on a credit is a definitive refusal with the problem type`() {
        server.stubFor(
            post(urlEqualTo("/accounts/$accountId/credits")).willReturn(problem(404, "account-not-found")),
        )

        val result = gateway.perform(AccountsCall.Credit(accountId, Money(5), "transfer-1-credit", "d"))

        assertThat(result.outcome).isEqualTo(StepOutcome.Refused("account-not-found"))
    }

    @Test
    fun `422 idempotency-key-reused is a definitive refusal with the problem type`() {
        server.stubFor(
            post(urlEqualTo("/accounts/$accountId/debits")).willReturn(problem(422, "idempotency-key-reused")),
        )

        assertThat(gateway.perform(debit()).outcome).isEqualTo(StepOutcome.Refused("idempotency-key-reused"))
    }

    @Test
    fun `503 account-busy is transient`() {
        server.stubFor(
            post(urlEqualTo("/accounts/$accountId/debits"))
                .willReturn(problem(503, "account-busy").withHeader("Retry-After", "1")),
        )

        val result = gateway.perform(debit())

        assertThat(result.outcome).isEqualTo(StepOutcome.TransientFailure)
        assertThat(result.httpStatus).isEqualTo(503)
    }

    @Test
    fun `find entry sends the key as a query parameter and 200 is success`() {
        server.stubFor(
            get(urlPathEqualTo("/accounts/$accountId/entries"))
                .willReturn(aResponse().withStatus(200).withBody(entryJson())),
        )
        MDC.put("correlationId", "corr-9")

        val result = gateway.perform(AccountsCall.FindEntry(accountId, "transfer-1-debit"))

        assertThat(result.outcome).isEqualTo(StepOutcome.Success)
        server.verify(
            getRequestedFor(urlEqualTo("/accounts/$accountId/entries?idempotencyKey=transfer-1-debit"))
                .withHeader("X-Correlation-Id", equalTo("corr-9")),
        )
    }

    @Test
    fun `find entry encodes the key in the query string`() {
        server.stubFor(
            get(urlPathEqualTo("/accounts/$accountId/entries")).willReturn(aResponse().withStatus(200).withBody(entryJson())),
        )

        gateway.perform(AccountsCall.FindEntry(accountId, "a&b=c d"))

        server.verify(
            getRequestedFor(urlPathEqualTo("/accounts/$accountId/entries"))
                .withQueryParam("idempotencyKey", equalTo("a&b=c d")),
        )
    }

    @Test
    fun `404 entry-not-found on find entry is the entry-not-found refusal`() {
        server.stubFor(
            get(urlPathEqualTo("/accounts/$accountId/entries")).willReturn(problem(404, "entry-not-found")),
        )

        val result = gateway.perform(AccountsCall.FindEntry(accountId, "transfer-1-debit"))

        assertThat(result.outcome).isEqualTo(StepOutcome.Refused(StepOutcome.ENTRY_NOT_FOUND))
        assertThat(result.httpStatus).isEqualTo(404)
    }

    @Test
    fun `a response slower than the read timeout is transient and returns near the timeout`() {
        server.stubFor(
            post(urlEqualTo("/accounts/$accountId/debits"))
                .willReturn(aResponse().withStatus(201).withBody(entryJson()).withFixedDelay(10_000)),
        )

        val started = System.nanoTime()
        val result = gateway.perform(debit())
        val elapsed = Duration.ofNanos(System.nanoTime() - started)

        assertThat(result.outcome).isEqualTo(StepOutcome.TransientFailure)
        assertThat(result.httpStatus).isNull()
        assertThat(elapsed).isBetween(Duration.ofMillis(2_800), Duration.ofMillis(4_500))
    }

    @Test
    fun `find entry with 404 account-not-found is that refusal, not entry-not-found`() {
        server.stubFor(
            get(urlPathEqualTo("/accounts/$accountId/entries")).willReturn(problem(404, "account-not-found")),
        )

        val result = gateway.perform(AccountsCall.FindEntry(accountId, "k"))

        assertThat(result.outcome).isEqualTo(StepOutcome.Refused("account-not-found"))
    }

    @Test
    fun `find entry with 404 in HTML is the http-404 refusal`() {
        server.stubFor(
            get(urlPathEqualTo("/accounts/$accountId/entries"))
                .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "text/html").withBody("<html>not found</html>")),
        )

        assertThat(gateway.perform(AccountsCall.FindEntry(accountId, "k")).outcome).isEqualTo(StepOutcome.Refused("http-404"))
    }

    @Test
    fun `404 Problem Details with type about-blank is the http-404 refusal`() {
        server.stubFor(
            get(urlPathEqualTo("/accounts/$accountId/entries")).willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/problem+json")
                    .withBody("""{"type":"about:blank","title":"Not Found","status":404}"""),
            ),
        )

        assertThat(gateway.perform(AccountsCall.FindEntry(accountId, "k")).outcome).isEqualTo(StepOutcome.Refused("http-404"))
    }

    @Test
    fun `a body that stalls after a fast status line is transient within the total deadline`() {
        // Raw socket: the headers arrive at once, then the body promises 500 bytes and sends only a few.
        ServerSocket(0).use { stalling ->
            val peer =
                thread(isDaemon = true) {
                    runCatching {
                        stalling.accept().use { socket ->
                            socket.getInputStream().read(ByteArray(4_096))
                            val out = socket.getOutputStream()
                            out.write(
                                ("HTTP/1.1 201 Created\r\nContent-Type: application/json\r\nContent-Length: 500\r\n\r\n{\"id\":")
                                    .toByteArray(),
                            )
                            out.flush()
                            repeat(10) {
                                Thread.sleep(1_000)
                                out.write(' '.code)
                                out.flush()
                            }
                        }
                    }
                }

            val started = System.nanoTime()
            val result =
                try {
                    gatewayFor("http://localhost:${stalling.localPort}").perform(debit())
                } finally {
                    peer.interrupt()
                }
            val elapsed = Duration.ofNanos(System.nanoTime() - started)

            assertThat(result.outcome).isEqualTo(StepOutcome.TransientFailure)
            assertThat(result.httpStatus).isNull()
            assertThat(elapsed).isBetween(Duration.ofMillis(2_800), Duration.ofMillis(4_500))
        }
    }

    @Test
    fun `502 with an HTML body from the proxy is transient`() {
        server.stubFor(
            post(urlEqualTo("/accounts/$accountId/debits"))
                .willReturn(
                    aResponse()
                        .withStatus(502)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html><body><h1>502 Bad Gateway</h1></body></html>"),
                ),
        )

        val result = gateway.perform(debit())

        assertThat(result.outcome).isEqualTo(StepOutcome.TransientFailure)
        assertThat(result.httpStatus).isEqualTo(502)
    }

    @Test
    fun `200 with a body that is not JSON is transient, not a success`() {
        server.stubFor(
            post(urlEqualTo("/accounts/$accountId/debits"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/html").withBody("<html>captive portal</html>")),
        )

        val result = gateway.perform(debit())

        assertThat(result.outcome).isEqualTo(StepOutcome.TransientFailure)
        assertThat(result.httpStatus).isEqualTo(200)
    }

    @Test
    fun `200 with an empty body or a JSON without an entry id is transient`() {
        server.stubFor(post(urlEqualTo("/accounts/$accountId/debits")).willReturn(aResponse().withStatus(201)))
        server.stubFor(
            get(urlPathEqualTo("/accounts/$accountId/entries"))
                .willReturn(aResponse().withStatus(200).withBody("""{"unexpected":true}""")),
        )

        assertThat(gateway.perform(debit()).outcome).isEqualTo(StepOutcome.TransientFailure)
        assertThat(gateway.perform(AccountsCall.FindEntry(accountId, "k")).outcome).isEqualTo(StepOutcome.TransientFailure)
    }

    @Test
    fun `a connection cut by the peer is transient without an exception`() {
        server.stubFor(
            post(urlEqualTo("/accounts/$accountId/debits"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)),
        )

        val result = gateway.perform(debit())

        assertThat(result.outcome).isEqualTo(StepOutcome.TransientFailure)
        assertThat(result.httpStatus).isNull()
    }

    @Test
    fun `an empty response and a malformed chunk are transient`() {
        server.stubFor(
            post(urlEqualTo("/accounts/$accountId/debits")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)),
        )
        server.stubFor(
            post(urlEqualTo("/accounts/$accountId/credits")).willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)),
        )

        assertThat(gateway.perform(debit()).outcome).isEqualTo(StepOutcome.TransientFailure)
        assertThat(gateway.perform(AccountsCall.Credit(accountId, Money(1), "k", "d")).outcome)
            .isEqualTo(StepOutcome.TransientFailure)
    }

    @Test
    fun `connection refused is transient`() {
        val closedPort = server.port()
        server.stop()

        val result = gatewayFor("http://localhost:$closedPort").perform(debit())

        assertThat(result.outcome).isEqualTo(StepOutcome.TransientFailure)
        assertThat(result.httpStatus).isNull()
    }

    @Test
    fun `lookup account returns EXISTS for 200`() {
        server.stubFor(
            get(urlEqualTo("/accounts/$accountId")).willReturn(
                aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                    """{"id":"$accountId","customerId":"$entryId","type":"FREE","balance":0,"overdraftLimit":0,"availableBalance":0}""",
                ),
            ),
        )
        MDC.put("correlationId", "corr-7")

        assertThat(gateway.lookupAccount(accountId)).isEqualTo(AccountLookup.EXISTS)
        server.verify(getRequestedFor(urlEqualTo("/accounts/$accountId")).withHeader("X-Correlation-Id", equalTo("corr-7")))
    }

    @Test
    fun `lookup account returns NOT_FOUND for 404 account-not-found`() {
        server.stubFor(get(urlEqualTo("/accounts/$accountId")).willReturn(problem(404, "account-not-found")))

        assertThat(gateway.lookupAccount(accountId)).isEqualTo(AccountLookup.NOT_FOUND)
    }

    @Test
    fun `lookup account returns UNAVAILABLE for 5xx, bad 200 body, cut connection and timeout`() {
        server.stubFor(get(urlEqualTo("/accounts/$accountId")).willReturn(aResponse().withStatus(503)))
        assertThat(gateway.lookupAccount(accountId)).isEqualTo(AccountLookup.UNAVAILABLE)

        server.stubFor(get(urlEqualTo("/accounts/$accountId")).willReturn(aResponse().withStatus(200).withBody("<html></html>")))
        assertThat(gateway.lookupAccount(accountId)).isEqualTo(AccountLookup.UNAVAILABLE)

        server.stubFor(get(urlEqualTo("/accounts/$accountId")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)))
        assertThat(gateway.lookupAccount(accountId)).isEqualTo(AccountLookup.UNAVAILABLE)

        server.stubFor(get(urlEqualTo("/accounts/$accountId")).willReturn(aResponse().withStatus(200).withFixedDelay(4_000)))
        assertThat(gateway.lookupAccount(accountId)).isEqualTo(AccountLookup.UNAVAILABLE)
    }

    @Test
    fun `lookup account fails loudly on any other 4xx because it points to a bug`() {
        server.stubFor(get(urlEqualTo("/accounts/$accountId")).willReturn(problem(400, "validation-error")))

        assertThatThrownBy { gateway.lookupAccount(accountId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("400")
            .hasMessageContaining("validation-error")
    }
}
