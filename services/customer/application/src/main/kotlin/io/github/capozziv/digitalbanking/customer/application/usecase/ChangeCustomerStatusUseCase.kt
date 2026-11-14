package io.github.capozziv.digitalbanking.customer.application.usecase

import io.github.capozziv.digitalbanking.customer.application.CustomerNotFoundException
import io.github.capozziv.digitalbanking.customer.application.port.CustomerRepository
import io.github.capozziv.digitalbanking.customer.application.port.EventOutbox
import io.github.capozziv.digitalbanking.customer.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.customer.domain.Customer
import io.github.capozziv.digitalbanking.customer.domain.CustomerStatus
import java.time.Clock
import java.util.UUID

/**
 * Sets the status of a customer (admin). The row is locked, so two concurrent changes apply one after the other and
 * each writes the event of the transition it made. The same status again changes nothing and writes no event.
 */
class ChangeCustomerStatusUseCase(
    private val customers: CustomerRepository,
    private val outbox: EventOutbox,
    private val tx: TransactionRunner,
    private val clock: Clock,
) {
    fun execute(
        id: UUID,
        status: CustomerStatus,
    ): Customer =
        tx.inTransaction {
            val current = customers.lockById(id) ?: throw CustomerNotFoundException(id)
            val (changed, event) = current.changeStatus(status, clock.instant())
            if (event != null) {
                customers.update(changed)
                outbox.append(event)
            }
            changed
        }
}
