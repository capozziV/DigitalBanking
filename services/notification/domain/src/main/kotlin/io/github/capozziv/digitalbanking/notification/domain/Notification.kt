package io.github.capozziv.digitalbanking.notification.domain

import java.time.Instant
import java.util.UUID

data class Notification(
    val id: UUID,
    val customerId: UUID,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val data: Map<String, String>,
    val sourceEventId: UUID,
    val createdAt: Instant,
    val readAt: Instant?,
) {
    /** Idempotent: an already read notification keeps its original [readAt]. */
    fun markRead(at: Instant): Notification = if (readAt != null) this else copy(readAt = at)
}
