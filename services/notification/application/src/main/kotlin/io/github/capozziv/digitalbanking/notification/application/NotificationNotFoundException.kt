package io.github.capozziv.digitalbanking.notification.application

import java.util.UUID

/** The notification does not exist or belongs to another customer. */
class NotificationNotFoundException(
    val notificationId: UUID,
) : RuntimeException("notification $notificationId not found")
