package io.github.capozziv.digitalbanking.notification.adapters.web

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
 * Minimal correlation ID filter, a copy of the one in accounts and transfers (all move to libs/observabilidade in
 * stage 7). Reuses `X-Correlation-Id` when it is a safe token, otherwise generates a UUID; puts it in the MDC (key
 * `correlationId`, the one the JSON logs print) and returns it in the response header.
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
        MDC.put(MDC_KEY, correlationId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_KEY)
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
        const val MDC_KEY = "correlationId"

        /** Letters, digits and `. _ - = + /` (base64 ids of the API Gateway), up to 128 characters. */
        val SAFE_ID = Regex("[A-Za-z0-9._\\-=+/]{1,128}")

        val log = LoggerFactory.getLogger(CorrelationIdFilter::class.java)
    }
}
