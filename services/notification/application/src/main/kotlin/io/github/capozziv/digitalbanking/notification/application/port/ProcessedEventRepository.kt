package io.github.capozziv.digitalbanking.notification.application.port

import java.time.Instant
import java.util.UUID

interface ProcessedEventRepository {
    /** Records [eventId]; false when it was already recorded, so the event must not be applied again. */
    fun markProcessed(
        eventId: UUID,
        eventType: String,
        at: Instant,
    ): Boolean
}
