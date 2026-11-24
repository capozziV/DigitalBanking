package io.github.capozziv.digitalbanking.accounts.bootstrap.web

import io.github.capozziv.digitalbanking.accounts.bootstrap.AccountsIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import java.util.UUID

/**
 * The trace of a request goes on with the calls of `accounts` to other services: the lookup of the customer when an
 * account opens and the lookup of pending transfers when it closes each carry a W3C `traceparent` of the trace of the
 * inbound request (a client span of its own as the parent), next to the `X-Correlation-Id`.
 */
class HttpTracePropagationIntegrationTest : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }

    @Test
    fun `the customer lookup carries the traceparent of the inbound request and the correlation id`() {
        val customerId = UUID.randomUUID()

        val response =
            api.send(
                HttpMethod.POST,
                "/accounts",
                headers = asCustomer(customerId) + mapOf("Idempotency-Key" to UUID.randomUUID().toString()) + traced("open-trace"),
            )

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(201)
        val lookup = customerService.lookupsOf(customerId).single()
        assertThat(lookup.traceparent).matches(CHILD_OF_INBOUND)
        // The parent is the client span of the call, not the span of the caller.
        assertThat(lookup.traceparent).doesNotContain(INBOUND_SPAN_ID)
        assertThat(lookup.correlationId).isEqualTo("open-trace")
    }

    @Test
    fun `the pending transfers lookup carries the traceparent of the inbound request and the correlation id`() {
        val customerId = UUID.randomUUID()
        val accountId =
            api
                .openAccount(customerId)
                .body!!
                .get("id")
                .asString()

        val response = api.send(HttpMethod.POST, "/accounts/$accountId/close", headers = asCustomer(customerId) + traced("close-trace"))

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        val lookup = transfersService.lookupsOf(UUID.fromString(accountId)).single()
        assertThat(lookup.getHeader("traceparent")).matches(CHILD_OF_INBOUND)
        assertThat(lookup.getHeader("traceparent")).doesNotContain(INBOUND_SPAN_ID)
        assertThat(lookup.getHeader("X-Correlation-Id")).isEqualTo("close-trace")
    }

    private fun traced(correlationId: String) =
        mapOf("traceparent" to "00-$INBOUND_TRACE_ID-$INBOUND_SPAN_ID-01", "X-Correlation-Id" to correlationId)

    private companion object {
        const val INBOUND_TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736"
        const val INBOUND_SPAN_ID = "00f067aa0ba902b7"
        const val CHILD_OF_INBOUND = "00-$INBOUND_TRACE_ID-[0-9a-f]{16}-01"
    }
}
