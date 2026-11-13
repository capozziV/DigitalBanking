package io.github.capozziv.digitalbanking.observability

/** Names shared by every hop of the correlation ID: HTTP header, MDC key (printed by the JSON logs). */
object CorrelationId {
    const val HEADER = "X-Correlation-Id"

    /** Same value as `OutboxEventEnvelope.CORRELATION_ID_MDC_KEY`, where the outbox writer reads it (checked by a test). */
    const val MDC_KEY = "correlationId"
}
