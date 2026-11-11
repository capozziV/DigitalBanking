package io.github.capozziv.digitalbanking.notification.adapters.web.dto

import io.github.capozziv.digitalbanking.notification.domain.Notification
import io.github.capozziv.digitalbanking.notification.domain.NotificationKind
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

/**
 * One notification of the bell. `readAt` is null while unread; `data` holds the identifiers (strings) and the
 * amounts in cents (integers) of the event.
 */
data class NotificationDTO(
    val id: UUID,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    @field:Schema(
        description = "Event details: identifiers as text, amounts in cents as numbers.",
        type = "object",
        example = "{\"transferId\": \"t-1\", \"amountCents\": 12345}",
    )
    val data: Map<String, Any>,
    val createdAt: Instant,
    val readAt: Instant?,
) {
    companion object {
        fun from(notification: Notification) =
            NotificationDTO(
                id = notification.id,
                kind = notification.kind,
                title = notification.title,
                body = notification.body,
                data = notification.data,
                createdAt = notification.createdAt,
                readAt = notification.readAt,
            )
    }
}
