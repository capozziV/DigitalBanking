package io.github.capozziv.digitalbanking.notification.application

import io.github.capozziv.digitalbanking.security.Caller
import java.util.UUID

/**
 * The customer whose notifications a caller reads and marks by default: a customer is themselves, an admin is the
 * user of their own token (an admin is a customer of the bank too). A service has no notifications.
 */
internal fun Caller.ownCustomerId(): UUID =
    when (this) {
        is Caller.Customer -> customerId
        is Caller.Admin -> userId
        Caller.Service -> throw ForeignCustomerException()
    }
