package io.github.capozziv.digitalbanking.transfers.application.port

import java.util.UUID

/** Status of a customer as the `customer` service answers it now; never cached. */
enum class CustomerLookup { ACTIVE, BLOCKED, NOT_FOUND, UNAVAILABLE }

/** Calls to the `customer` service, the source of truth of the customer status. */
interface CustomerGateway {
    /** [CustomerLookup.UNAVAILABLE] on timeout, error or any answer that is not a customer or a 404. */
    fun lookupCustomer(id: UUID): CustomerLookup
}
