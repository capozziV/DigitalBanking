package io.github.capozziv.digitalbanking.notification.application.port

import io.github.capozziv.digitalbanking.notification.domain.Notification
import java.time.Instant
import java.util.UUID

/** Position after which the next page starts, exclusive, in (createdAt desc, id desc) order. */
data class NotificationCursor(
    val createdAt: Instant,
    val id: UUID,
)

interface NotificationRepository {
    fun saveAll(notifications: List<Notification>)

    /** Up to [size] notifications of [customerId], newest first, strictly after [cursor] when given. */
    fun page(
        customerId: UUID,
        cursor: NotificationCursor?,
        size: Int,
    ): List<Notification>

    fun countUnread(customerId: UUID): Long

    fun find(id: UUID): Notification?

    /** Marks unread notifications of [customerId] up to [upTo] inclusive; returns how many changed. */
    fun markReadUpTo(
        customerId: UUID,
        upTo: Notification,
        at: Instant,
    ): Int
}
