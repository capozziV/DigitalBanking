package io.github.capozziv.digitalbanking.outbox.relay.web.dto

import io.github.capozziv.digitalbanking.outbox.relay.StuckEvent
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

/** An outbox event that failed to publish and keeps the later events of its aggregate waiting. */
data class StuckOutboxEventDTO(
    val eventId: UUID,
    val aggregateId: String,
    val eventType: String,
    val occurredAt: Instant,
    @field:Schema(description = "Failed publication attempts so far")
    val attempts: Int,
    @field:Schema(description = "Error of the last attempt, as the relay recorded it")
    val lastError: String?,
) {
    companion object {
        fun from(event: StuckEvent) =
            StuckOutboxEventDTO(
                eventId = event.eventId,
                aggregateId = event.aggregateId,
                eventType = event.eventType,
                occurredAt = event.occurredAt,
                attempts = event.attempts,
                lastError = event.lastError,
            )
    }
}
