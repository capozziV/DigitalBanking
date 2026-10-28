package io.github.capozziv.digitalbanking.notification.application.usecase

import io.github.capozziv.digitalbanking.notification.application.port.NotificationCursor
import io.github.capozziv.digitalbanking.notification.application.port.NotificationRepository
import io.github.capozziv.digitalbanking.notification.domain.Notification
import java.util.UUID

data class NotificationPage(
    val items: List<Notification>,
    val unreadCount: Long,
    val nextCursor: NotificationCursor?,
)

class ListNotificationsUseCase(
    private val notifications: NotificationRepository,
) {
    fun list(
        customerId: UUID,
        cursor: NotificationCursor?,
    ): NotificationPage {
        // One extra row tells whether another page exists without a count query.
        val fetched = notifications.page(customerId, cursor, PAGE_SIZE + 1)
        val items = fetched.take(PAGE_SIZE)
        val nextCursor =
            if (fetched.size > PAGE_SIZE) items.last().let { NotificationCursor(it.createdAt, it.id) } else null
        return NotificationPage(items, notifications.countUnread(customerId), nextCursor)
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
