package io.github.capozziv.digitalbanking.customer.application

import io.github.capozziv.digitalbanking.customer.domain.PhoneNumber
import java.util.UUID

class CustomerNotFoundException(
    val customerId: UUID,
) : RuntimeException("customer $customerId not found")

class CustomerNotFoundByPhoneException(
    val phone: PhoneNumber,
) : RuntimeException("no customer has the phone $phone")

/** Another customer already has the phone: it is unique among customers. */
class PhoneAlreadyInUseException(
    val phone: PhoneNumber,
) : RuntimeException("the phone $phone is already in use")

/** A service caller (machine token) has no customer of its own: it cannot register one or read "me". */
class CallerHasNoCustomerException : RuntimeException("the caller is not a customer")
