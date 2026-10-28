package io.github.capozziv.digitalbanking.transfers.adapters.correlation

import io.github.capozziv.digitalbanking.outbox.OutboxEventEnvelope.CORRELATION_ID_MDC_KEY
import io.github.capozziv.digitalbanking.transfers.application.port.CorrelationContext
import org.slf4j.MDC
import org.springframework.stereotype.Component

/**
 * Correlation ID in the MDC, under the key the log pattern, the accounts client and the outbox writer read.
 * Used where no request filter set it (the saga scheduler).
 */
@Component
class MdcCorrelationContext : CorrelationContext {
    override fun <T> withCorrelationId(
        correlationId: String?,
        block: () -> T,
    ): T {
        val previous = MDC.get(CORRELATION_ID_MDC_KEY)
        put(correlationId)
        try {
            return block()
        } finally {
            put(previous)
        }
    }

    private fun put(correlationId: String?) {
        if (correlationId == null) MDC.remove(CORRELATION_ID_MDC_KEY) else MDC.put(CORRELATION_ID_MDC_KEY, correlationId)
    }
}
