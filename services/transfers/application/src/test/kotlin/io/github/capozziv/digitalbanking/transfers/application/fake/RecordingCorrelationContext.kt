package io.github.capozziv.digitalbanking.transfers.application.fake

import io.github.capozziv.digitalbanking.transfers.application.port.CorrelationContext

/** Keeps the current correlation ID in a field, as the MDC would, and restores the previous one afterwards. */
class RecordingCorrelationContext : CorrelationContext {
    var current: String? = null
        private set

    override fun <T> withCorrelationId(
        correlationId: String?,
        block: () -> T,
    ): T {
        val previous = current
        current = correlationId
        try {
            return block()
        } finally {
            current = previous
        }
    }
}
