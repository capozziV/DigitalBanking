package io.github.capozziv.digitalbanking.notification.application.usecase

import io.github.capozziv.digitalbanking.notification.application.NotificationNotFoundException
import io.github.capozziv.digitalbanking.notification.application.port.NotificationRepository
import io.github.capozziv.digitalbanking.notification.application.port.TransactionRunner
import java.time.Clock
import java.util.UUID

class MarkNotificationsReadUseCase(
    private val notifications: NotificationRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock,
) {
    /** Marks every unread notification of [customerId] up to [upTo] inclusive; returns how many changed. */
    fun execute(
        customerId: UUID,
        upTo: UUID,
    ): Int =
        transactions.inTransaction {
            val anchor =
                notifications.find(upTo)?.takeIf { it.customerId == customerId }
                    ?: throw NotificationNotFoundException(upTo)
            notifications.markReadUpTo(customerId, anchor, clock.instant())
        }
}
