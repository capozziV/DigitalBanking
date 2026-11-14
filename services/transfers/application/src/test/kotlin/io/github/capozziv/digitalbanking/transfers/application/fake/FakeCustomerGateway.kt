package io.github.capozziv.digitalbanking.transfers.application.fake

import io.github.capozziv.digitalbanking.transfers.application.port.CustomerGateway
import io.github.capozziv.digitalbanking.transfers.application.port.CustomerLookup
import java.util.UUID

/** Answers every lookup with [result] (an active customer by default) and records the ids asked for. */
class FakeCustomerGateway : CustomerGateway {
    var result = CustomerLookup.ACTIVE
    val lookups = mutableListOf<UUID>()

    override fun lookupCustomer(id: UUID): CustomerLookup {
        lookups += id
        return result
    }
}
