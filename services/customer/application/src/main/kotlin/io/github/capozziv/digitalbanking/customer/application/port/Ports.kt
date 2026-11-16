package io.github.capozziv.digitalbanking.customer.application.port

import io.github.capozziv.digitalbanking.customer.domain.Customer
import io.github.capozziv.digitalbanking.customer.domain.CustomerEvent
import java.util.UUID

interface CustomerRepository {
    /** Inserts the customer unless one with the same id exists; true when it was inserted. Never aborts the transaction. */
    fun insertIfAbsent(customer: Customer): Boolean

    fun findById(id: UUID): Customer?

    /** Reads the customer taking a row lock until the end of the transaction. */
    fun lockById(id: UUID): Customer?

    /** Writes the status and the update time of an existing customer. */
    fun update(customer: Customer)
}

/** Records the event in the outbox, in the transaction of the caller. */
interface EventOutbox {
    fun append(event: CustomerEvent)
}

interface TransactionRunner {
    fun <T> inTransaction(block: () -> T): T

    fun <T> inReadOnlyTransaction(block: () -> T): T
}
