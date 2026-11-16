package io.github.capozziv.digitalbanking.notification.application.usecase

import io.github.capozziv.digitalbanking.notification.application.NotificationNotFoundException
import io.github.capozziv.digitalbanking.notification.application.ownCustomerId
import io.github.capozziv.digitalbanking.notification.application.port.NotificationRepository
import io.github.capozziv.digitalbanking.notification.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.security.Caller
import java.time.Clock
import java.util.UUID

class MarkNotificationsReadUseCase(
    private val notifications: NotificationRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock,
) {
    /**
     * Marks every unread notification of the caller up to [upTo] inclusive; returns how many changed. Always the
     * caller's own (an admin included): reading another customer's list is an admin feature, changing it is not.
     */
    fun execute(
        caller: Caller,
        upTo: UUID,
    ): Int =
        transactions.inTransaction {
            val customerId = caller.ownCustomerId()
            val anchor =
                notifications.find(upTo)?.takeIf { it.customerId == customerId }
                    ?: throw NotificationNotFoundException(upTo)
            notifications.markReadUpTo(customerId, anchor, clock.instant())
        }
}
