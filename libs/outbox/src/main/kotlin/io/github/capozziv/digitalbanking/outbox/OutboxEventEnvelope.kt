package io.github.capozziv.digitalbanking.outbox

import tools.jackson.databind.json.JsonMapper

/**
 * Envelope of the events published from the outbox: eventId, eventType, eventVersion, occurredAt,
 * aggregateId, correlationId and the payload as a JSON object.
 */
object OutboxEventEnvelope {
    /** MDC key holding the correlation ID of the current request. */
    const val CORRELATION_ID_MDC_KEY = "correlationId"

    private val jsonMapper = JsonMapper.builder().build()

    fun toJson(event: PendingEvent): String {
        val envelope =
            jsonMapper
                .createObjectNode()
                .put("eventId", event.eventId.toString())
                .put("eventType", event.eventType)
                .put("eventVersion", event.eventVersion)
                .put("occurredAt", event.occurredAt.toString())
                .put("aggregateId", event.aggregateId)
                .put("correlationId", event.correlationId)
        envelope.set("payload", jsonMapper.readTree(event.payloadJson))
        return jsonMapper.writeValueAsString(envelope)
    }
}
