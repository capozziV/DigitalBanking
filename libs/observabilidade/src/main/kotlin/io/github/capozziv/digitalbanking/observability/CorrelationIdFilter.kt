package io.github.capozziv.digitalbanking.observability

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Reuses the received `X-Correlation-Id` when it is a safe token, otherwise generates a UUID (and logs the
 * replacement at DEBUG, without the discarded value); puts it in the MDC under [CorrelationId.MDC_KEY], where the logs
 * and the outbox writer read it, for the whole request, and returns it in the response header.
 *
 * Registered by [ObservabilityAutoConfiguration] ahead of every other filter, Spring Security included, so a 401 or
 * 403 also carries the id.
 */
class CorrelationIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val correlationId = acceptedOrGenerated(request.getHeader(CorrelationId.HEADER))
        response.setHeader(CorrelationId.HEADER, correlationId)
        MDC.put(CorrelationId.MDC_KEY, correlationId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(CorrelationId.MDC_KEY)
        }
    }

    private fun acceptedOrGenerated(received: String?): String {
        if (received != null && SAFE_ID.matches(received)) return received
        val generated = UUID.randomUUID().toString()
        if (received != null) log.debug("discarded X-Correlation-Id that is not a safe token, generated {}", generated)
        return generated
    }

    private companion object {
        /**
         * Letters, digits and `. _ - = + /` (base64 ids of the API Gateway), up to 128 characters: nothing that
         * could forge log lines. The discarded value itself is not logged.
         */
        val SAFE_ID = Regex("[A-Za-z0-9._\\-=+/]{1,128}")

        val log = LoggerFactory.getLogger(CorrelationIdFilter::class.java)
    }
}
