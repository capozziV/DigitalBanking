package io.github.capozziv.digitalbanking.outbox

import java.time.Instant
import java.util.UUID

/** An outbox row not yet published, as the relay reads it. */
data class PendingEvent(
    val position: Long,
    val eventId: UUID,
    val aggregateId: String,
    val eventType: String,
    val eventVersion: Int,
    val payloadJson: String,
    val correlationId: String?,
    val occurredAt: Instant,
    val attempts: Int,
)
