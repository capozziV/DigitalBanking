package io.github.capozziv.digitalbanking.notification.application

import java.util.UUID

/** The event names an account whose `AccountOpened` has not been consumed yet; the message must be retried. */
class UnknownAccountException(
    val accountId: UUID,
) : RuntimeException("unknown account $accountId")
