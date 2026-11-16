package io.github.capozziv.digitalbanking.customer.application.usecase

import io.github.capozziv.digitalbanking.customer.application.CallerHasNoCustomerException
import io.github.capozziv.digitalbanking.customer.application.port.CustomerRepository
import io.github.capozziv.digitalbanking.customer.application.port.EventOutbox
import io.github.capozziv.digitalbanking.customer.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.customer.domain.Customer
import io.github.capozziv.digitalbanking.security.Caller
import java.time.Clock
import java.util.UUID

data class RegisterCustomerCommand(
    val name: String,
    val email: String,
)

data class RegisterCustomerResult(
    val customer: Customer,
    val created: Boolean,
)

/** The customer id of a caller that can own one: the `sub` of a customer or of an admin user. */
internal fun Caller.ownCustomerId(): UUID =
    when (this) {
        is Caller.Customer -> customerId
        is Caller.Admin -> userId
        Caller.Service -> throw CallerHasNoCustomerException()
    }

/**
 * Registers the customer of the caller, idempotent by the caller's `sub`: the first call creates it and writes
 * CustomerRegistered; a repeat returns the stored customer untouched, with no event.
 */
class RegisterCustomerUseCase(
    private val customers: CustomerRepository,
    private val outbox: EventOutbox,
    private val tx: TransactionRunner,
    private val clock: Clock,
) {
    fun execute(
        command: RegisterCustomerCommand,
        caller: Caller,
    ): RegisterCustomerResult {
        val id = caller.ownCustomerId()
        return tx.inTransaction {
            val (customer, event) = Customer.register(id, command.name, command.email, clock.instant())
            if (customers.insertIfAbsent(customer)) {
                outbox.append(event)
                RegisterCustomerResult(customer, created = true)
            } else {
                val existing = checkNotNull(customers.findById(id)) { "customer $id not found after a duplicate id conflict" }
                RegisterCustomerResult(existing, created = false)
            }
        }
    }
}
