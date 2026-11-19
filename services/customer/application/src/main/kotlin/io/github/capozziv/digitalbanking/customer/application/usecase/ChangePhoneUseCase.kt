package io.github.capozziv.digitalbanking.customer.application.usecase

import io.github.capozziv.digitalbanking.customer.application.CallerHasNoCustomerException
import io.github.capozziv.digitalbanking.customer.application.CustomerNotFoundException
import io.github.capozziv.digitalbanking.customer.application.PhoneAlreadyInUseException
import io.github.capozziv.digitalbanking.customer.application.port.CustomerRepository
import io.github.capozziv.digitalbanking.customer.application.port.EventOutbox
import io.github.capozziv.digitalbanking.customer.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.customer.domain.Customer
import io.github.capozziv.digitalbanking.customer.domain.PhoneNumber
import io.github.capozziv.digitalbanking.security.Caller
import java.time.Clock

/**
 * Changes the phone of the customer of the caller. The row is locked, so concurrent changes apply one after the
 * other. The same number again changes nothing and writes no event; a number another customer has throws
 * [PhoneAlreadyInUseException].
 */
class ChangePhoneUseCase(
    private val customers: CustomerRepository,
    private val outbox: EventOutbox,
    private val tx: TransactionRunner,
    private val clock: Clock,
) {
    fun execute(
        phone: PhoneNumber,
        caller: Caller,
    ): Customer {
        val id = caller.ownCustomerId() ?: throw CallerHasNoCustomerException()
        return tx.inTransaction {
            val current = customers.lockById(id) ?: throw CustomerNotFoundException(id)
            val (changed, event) = current.changePhone(phone, clock.instant())
            if (event != null) {
                customers.update(changed)
                outbox.append(event)
            }
            changed
        }
    }
}
