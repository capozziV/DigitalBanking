package io.github.capozziv.digitalbanking.accounts.application.port

import java.util.UUID

/** Status of a customer as the `customer` service reports it. */
enum class CustomerStatus { ACTIVE, BLOCKED }

/**
 * The `customer` service, asked on every account opening (no cache: it is the source of truth of the status).
 * Fails with `CustomersUnavailableException` when the service is down, slow or answers an error.
 */
interface CustomerGateway {
    /** The status of [customerId], or null when the customer does not exist. */
    fun findStatus(customerId: UUID): CustomerStatus?
}
