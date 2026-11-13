package io.github.capozziv.digitalbanking.security

import java.util.UUID

/**
 * Who is calling, resolved by `adapters/web` from a validated JWT and handed to the use cases.
 *
 * Plain Kotlin on purpose: the application module of each service depends on this type and must not see Spring.
 * - [Customer]: a user token without the admin group; the customer id is the token `sub`.
 * - [Admin]: a user token with the admin group; operates everything and, with [actsAsCustomer], its own accounts.
 * - [Service]: a machine token (client credentials) with the internal scope.
 */
sealed interface Caller {
    /** True when this caller may operate the resources of [customerId] as their owner. */
    fun actsAsCustomer(customerId: UUID): Boolean

    data class Customer(
        val customerId: UUID,
    ) : Caller {
        override fun actsAsCustomer(customerId: UUID) = this.customerId == customerId
    }

    /** An admin may also be a customer: the customer id of an admin user is its [userId] (the `sub`). */
    data class Admin(
        val userId: UUID,
    ) : Caller {
        override fun actsAsCustomer(customerId: UUID) = userId == customerId
    }

    data object Service : Caller {
        override fun actsAsCustomer(customerId: UUID) = false
    }
}
