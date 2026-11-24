package io.github.capozziv.digitalbanking.transfers.bootstrap

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import io.github.capozziv.digitalbanking.transfers.bootstrap.web.ApiClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import java.util.UUID

/**
 * The trace of a request goes on with the calls of the saga: each call to `accounts` and to `customer` carries a W3C
 * `traceparent` of the trace of the inbound request (a client span of its own as the parent), next to the
 * `X-Correlation-Id`.
 */
class HttpTracePropagationIntegrationTest : TransfersIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }

    private val source = UUID.randomUUID()
    private val destination = UUID.randomUUID()

    @BeforeEach
    fun stubAccounts() {
        accountsServer.resetAll()
        listOf(source, destination).forEach { id ->
            accountsServer.stubFor(
                get(urlEqualTo("/accounts/$id")).willReturn(
                    json(
                        200,
                        """{"id":"$id","customerId":"$CUSTOMER_ID","type":"FREE","balance":0,"overdraftLimit":0,"availableBalance":0}""",
                    ),
                ),
            )
        }
        listOf("/accounts/$source/debits", "/accounts/$destination/credits").forEach { path ->
            accountsServer.stubFor(
                post(urlEqualTo(path)).willReturn(
                    json(
                        201,
                        """{"id":"${UUID.randomUUID()}","accountId":"${UUID.randomUUID()}","sequence":1,"kind":"DEBIT",""" +
                            """"amount":1,"balanceAfter":0,"createdAt":"2026-10-27T10:00:00Z"}""",
                    ),
                ),
            )
        }
    }

    @Test
    fun `the calls to accounts and customer carry the traceparent of the inbound request and the correlation id`() {
        val response =
            api.post(
                "/transfers",
                """{"sourceAccountId":"$source","destinationAccountId":"$destination","amount":100}""",
                mapOf(
                    "Idempotency-Key" to UUID.randomUUID().toString(),
                    "traceparent" to "00-$INBOUND_TRACE_ID-$INBOUND_SPAN_ID-01",
                    "X-Correlation-Id" to "trace-propagation-test",
                ),
            )

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(201)
        val calls = callsOf(accountsServer) + callsOf(customerServer)
        assertThat(calls.map { it.url }).contains("/accounts/$source/debits", "/accounts/$destination/credits")
        assertThat(callsOf(customerServer)).isNotEmpty()
        calls.forEach { call ->
            val traceparent = call.getHeader("traceparent")
            assertThat(traceparent).describedAs(call.url).matches("00-$INBOUND_TRACE_ID-[0-9a-f]{16}-01")
            // The parent is the client span of the call, not the span of the caller.
            assertThat(traceparent).describedAs(call.url).doesNotContain(INBOUND_SPAN_ID)
            assertThat(call.getHeader("X-Correlation-Id")).describedAs(call.url).isEqualTo("trace-propagation-test")
        }
    }

    private fun callsOf(server: WireMockServer): List<LoggedRequest> =
        server.allServeEvents.map { it.request }.filter { it.url != ServiceTokenServers.TOKEN_PATH }

    private fun json(
        status: Int,
        body: String,
    ) = aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody(body)

    private companion object {
        const val INBOUND_TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736"
        const val INBOUND_SPAN_ID = "00f067aa0ba902b7"
    }
}
