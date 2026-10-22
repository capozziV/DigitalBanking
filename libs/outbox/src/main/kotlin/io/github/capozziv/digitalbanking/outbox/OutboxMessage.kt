package io.github.capozziv.digitalbanking.outbox

/** An event to be recorded in the outbox; the service provides only its type, version, aggregate and payload. */
data class OutboxMessage(
    val eventType: String,
    val eventVersion: Int,
    val aggregateId: String,
    val payloadJson: String,
)
