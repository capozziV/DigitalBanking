package io.github.capozziv.digitalbanking.accounts.application.fake

import io.github.capozziv.digitalbanking.accounts.application.CustomersUnavailableException
import io.github.capozziv.digitalbanking.accounts.application.port.CustomerGateway
import io.github.capozziv.digitalbanking.accounts.application.port.CustomerStatus
import java.util.UUID

/** Customer service stand-in: every customer is [CustomerStatus.ACTIVE] unless set otherwise; records the lookups. */
class StubCustomerGateway : CustomerGateway {
    private val statuses = mutableMapOf<UUID, CustomerStatus?>()
    private val unavailable = mutableSetOf<UUID>()
    val lookups = mutableListOf<UUID>()

    /** When set, each lookup is also recorded there as `customers.findStatus`. */
    var log: CallLog? = null

    fun set(
        customerId: UUID,
        status: CustomerStatus?,
    ) {
        statuses[customerId] = status
    }

    fun failFor(customerId: UUID) {
        unavailable += customerId
    }

    override fun findStatus(customerId: UUID): CustomerStatus? {
        lookups += customerId
        log?.record("customers.findStatus")
        if (customerId in unavailable) throw CustomersUnavailableException("customer service down")
        return if (customerId in statuses) statuses[customerId] else CustomerStatus.ACTIVE
    }
}
