package io.github.capozziv.digitalbanking.notification.adapters.web.dto

import io.github.capozziv.digitalbanking.notification.domain.Notification
import io.github.capozziv.digitalbanking.notification.domain.NotificationKind
import java.time.Instant
import java.util.UUID

/** One notification of the bell. `readAt` is null while unread; `data` holds the identifiers and amounts (cents) of the event. */
data class NotificationDTO(
    val id: UUID,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val data: Map<String, String>,
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
