package io.github.capozziv.digitalbanking.notification.adapters.web.dto

import io.github.capozziv.digitalbanking.notification.adapters.web.NotificationCursorCodec
import io.github.capozziv.digitalbanking.notification.application.usecase.NotificationPage

/** A page of notifications, newest first; `nextCursor` is null on the last page. */
data class NotificationPageDTO(
    val items: List<NotificationDTO>,
    val unreadCount: Long,
    val nextCursor: String?,
) {
    companion object {
        fun from(page: NotificationPage) =
            NotificationPageDTO(
                items = page.items.map(NotificationDTO::from),
                unreadCount = page.unreadCount,
                nextCursor = page.nextCursor?.let(NotificationCursorCodec::encode),
            )
    }
}
