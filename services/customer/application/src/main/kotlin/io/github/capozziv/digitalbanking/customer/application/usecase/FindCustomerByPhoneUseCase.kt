package io.github.capozziv.digitalbanking.customer.application.usecase

import io.github.capozziv.digitalbanking.customer.application.CustomerNotFoundByPhoneException
import io.github.capozziv.digitalbanking.customer.application.port.CustomerRepository
import io.github.capozziv.digitalbanking.customer.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.customer.domain.Customer
import io.github.capozziv.digitalbanking.customer.domain.PhoneNumber

/** Finds the customer that has the phone; the route that calls it decides who may. */
class FindCustomerByPhoneUseCase(
    private val customers: CustomerRepository,
    private val tx: TransactionRunner,
) {
    fun execute(phone: PhoneNumber): Customer =
        tx.inReadOnlyTransaction { customers.findByPhone(phone) } ?: throw CustomerNotFoundByPhoneException(phone)
}
