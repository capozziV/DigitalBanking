package io.github.capozziv.digitalbanking.customer.application

import java.util.UUID

class CustomerNotFoundException(
    val customerId: UUID,
) : RuntimeException("customer $customerId not found")

/** A service caller (machine token) has no customer of its own: it cannot register one or read "me". */
class CallerHasNoCustomerException : RuntimeException("the caller is not a customer")
