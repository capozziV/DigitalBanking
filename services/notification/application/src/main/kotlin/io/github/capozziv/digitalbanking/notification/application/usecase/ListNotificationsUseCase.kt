package io.github.capozziv.digitalbanking.notification.application.usecase

import io.github.capozziv.digitalbanking.notification.application.ForeignCustomerException
import io.github.capozziv.digitalbanking.notification.application.ownCustomerId
import io.github.capozziv.digitalbanking.notification.application.port.NotificationCursor
import io.github.capozziv.digitalbanking.notification.application.port.NotificationRepository
import io.github.capozziv.digitalbanking.notification.domain.Notification
import io.github.capozziv.digitalbanking.security.Caller
import java.util.UUID

data class NotificationPage(
    val items: List<Notification>,
    val unreadCount: Long,
    val nextCursor: NotificationCursor?,
)

class ListNotificationsUseCase(
    private val notifications: NotificationRepository,
) {
    /**
     * Lists the notifications of the caller. [requestedCustomerId] (the `customerId` query parameter) is for an admin
     * who reads the notifications of a customer; without it the caller reads their own. A customer who names anyone
     * but themselves gets [ForeignCustomerException].
     */
    fun execute(
        caller: Caller,
        requestedCustomerId: UUID?,
        cursor: NotificationCursor?,
    ): NotificationPage {
        val customerId = customerToRead(caller, requestedCustomerId)
        // One extra row tells whether another page exists without a count query.
        val fetched = notifications.page(customerId, cursor, PAGE_SIZE + 1)
        val items = fetched.take(PAGE_SIZE)
        val nextCursor =
            if (fetched.size > PAGE_SIZE) items.last().let { NotificationCursor(it.createdAt, it.id) } else null
        return NotificationPage(items, notifications.countUnread(customerId), nextCursor)
    }

    private fun customerToRead(
        caller: Caller,
        requestedCustomerId: UUID?,
    ): UUID {
        val own = caller.ownCustomerId()
        return when {
            requestedCustomerId == null || requestedCustomerId == own -> own
            caller is Caller.Admin -> requestedCustomerId
            else -> throw ForeignCustomerException()
        }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
