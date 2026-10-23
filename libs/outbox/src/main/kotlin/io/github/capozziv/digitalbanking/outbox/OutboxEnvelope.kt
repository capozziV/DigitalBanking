package io.github.capozziv.digitalbanking.outbox

/** Envelope of the events published from the outbox. The JSON serialization arrives with the relay. */
object OutboxEnvelope {
    /** MDC key holding the correlation ID of the current request. */
    const val CORRELATION_ID_MDC_KEY = "correlationId"
}
