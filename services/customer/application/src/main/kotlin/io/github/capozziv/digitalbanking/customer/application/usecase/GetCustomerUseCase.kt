package io.github.capozziv.digitalbanking.customer.application.usecase

import io.github.capozziv.digitalbanking.customer.application.CustomerNotFoundException
import io.github.capozziv.digitalbanking.customer.application.port.CustomerRepository
import io.github.capozziv.digitalbanking.customer.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.customer.domain.Customer
import io.github.capozziv.digitalbanking.security.Caller
import java.util.UUID

/** Reads a customer by id; the routes that call it (admin, internal) already restrict who may. */
class GetCustomerUseCase(
    private val customers: CustomerRepository,
    private val tx: TransactionRunner,
) {
    fun execute(id: UUID): Customer = tx.inReadOnlyTransaction { customers.findById(id) } ?: throw CustomerNotFoundException(id)
}

/** The customer of the caller: a customer, or an admin acting as the customer of its own userId. */
class GetOwnCustomerUseCase(
    private val getCustomer: GetCustomerUseCase,
) {
    fun execute(caller: Caller): Customer = getCustomer.execute(caller.ownCustomerId())
}
