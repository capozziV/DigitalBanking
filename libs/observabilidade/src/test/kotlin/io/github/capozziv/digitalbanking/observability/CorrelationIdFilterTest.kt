package io.github.capozziv.digitalbanking.observability

import io.github.capozziv.digitalbanking.outbox.OutboxEventEnvelope
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.UUID

class CorrelationIdFilterTest {
    private val filter = CorrelationIdFilter()

    @Test
    fun `the MDC key is the one the outbox writer reads`() {
        assertThat(CorrelationId.MDC_KEY).isEqualTo(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY)
        assertThat(CorrelationId.HEADER).isEqualTo("X-Correlation-Id")
    }

    @Test
    fun `a safe received id is reused in the MDC during the request and in the response header`() {
        val received = "Root=1-abc_DEF.9+x/y=="

        val (seenInMdc, response) = run(received)

        assertThat(seenInMdc).isEqualTo(received)
        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo(received)
    }

    @Test
    fun `without the header a UUID is generated`() {
        val (seenInMdc, response) = run(null)

        assertThat(UUID.fromString(seenInMdc)).isNotNull()
        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo(seenInMdc)
    }

    @Test
    fun `an id with characters that could forge log lines is replaced by a UUID`() {
        val (seenInMdc, response) = run("abc\nERROR forged")

        assertThat(UUID.fromString(seenInMdc)).isNotNull()
        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo(seenInMdc)
    }

    @Test
    fun `an id of 128 characters is kept and one of 129 is replaced`() {
        assertThat(run("a".repeat(128)).first).isEqualTo("a".repeat(128))
        assertThat(UUID.fromString(run("a".repeat(129)).first)).isNotNull()
    }

    @Test
    fun `an empty id is replaced by a UUID`() {
        assertThat(UUID.fromString(run("").first)).isNotNull()
    }

    @Test
    fun `the MDC is cleaned after the request, even when the chain throws`() {
        val request = MockHttpServletRequest().apply { addHeader(CorrelationId.HEADER, "abc") }
        val failing = FilterChain { _, _ -> error("boom") }

        assertThatThrownBy { filter.doFilter(request, MockHttpServletResponse(), failing) }.hasMessage("boom")
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull()

        run("def")
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull()
    }

    private fun run(received: String?): Pair<String, MockHttpServletResponse> {
        val request = MockHttpServletRequest()
        if (received != null) request.addHeader(CorrelationId.HEADER, received)
        val response = MockHttpServletResponse()
        var seen: String? = null
        filter.doFilter(request, response) { _, _ -> seen = MDC.get(CorrelationId.MDC_KEY) }
        return checkNotNull(seen) to response
    }
}
