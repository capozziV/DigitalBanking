package io.github.capozziv.digitalbanking.accounts.adapters.web

import io.github.capozziv.digitalbanking.outbox.OutboxEnvelope
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Minimal correlation ID filter (moves to libs/observabilidade in stage 7). Reuses `X-Correlation-Id` when
 * it is a safe token, otherwise generates a UUID (and logs the replacement at DEBUG); puts it in the MDC, where JdbcOutboxWriter reads it, and
 * returns it in the response header.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val correlationId = acceptedOrGenerated(request.getHeader(HEADER))
        response.setHeader(HEADER, correlationId)
        MDC.put(OutboxEnvelope.CORRELATION_ID_MDC_KEY, correlationId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(OutboxEnvelope.CORRELATION_ID_MDC_KEY)
        }
    }

    private fun acceptedOrGenerated(received: String?): String {
        if (received != null && SAFE_ID.matches(received)) return received
        val generated = UUID.randomUUID().toString()
        if (received != null) log.debug("discarded X-Correlation-Id that is not a safe token, generated {}", generated)
        return generated
    }

    private companion object {
        const val HEADER = "X-Correlation-Id"

        /**
         * Letters, digits and `. _ - = + /` (base64 ids of the API Gateway), up to 128 characters: nothing that
         * could forge log lines. The discarded value itself is not logged.
         */
        val SAFE_ID = Regex("[A-Za-z0-9._\\-=+/]{1,128}")

        val log = LoggerFactory.getLogger(CorrelationIdFilter::class.java)
    }
}
