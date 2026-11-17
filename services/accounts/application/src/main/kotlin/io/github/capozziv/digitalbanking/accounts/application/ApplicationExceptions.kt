package io.github.capozziv.digitalbanking.accounts.application

import java.util.UUID

class AccountNotFoundException(
    val accountId: UUID,
) : RuntimeException("account $accountId not found")

class AccountBusyException(
    val accountId: UUID,
) : RuntimeException("account $accountId is busy")

class IdempotencyKeyReusedException(
    val key: String,
) : RuntimeException("idempotency key $key was already used for another operation")

class CustomerAlreadyHasAccountException(
    val customerId: UUID,
) : RuntimeException("customer $customerId already has an account")

class CustomerNotFoundException(
    val customerId: UUID,
) : RuntimeException("customer $customerId not found")

class CustomerBlockedException(
    val customerId: UUID,
) : RuntimeException("customer $customerId is blocked")

/** The `customer` service could not answer (down, timeout, 5xx, no service token). */
class CustomersUnavailableException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** The caller is no customer (a service), so it has no account of its own to open. */
class CallerNotCustomerException : RuntimeException("the caller is not a customer")

/** The account still has [count] outgoing transfers PENDING in the `transfers` service, so it cannot be closed yet. */
class PendingTransfersException(
    val accountId: UUID,
    val count: Int,
) : RuntimeException("account $accountId has $count pending outgoing transfers")

/** The `transfers` service could not answer (down, timeout, error, no service token); nothing was written. */
class TransfersUnavailableException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
