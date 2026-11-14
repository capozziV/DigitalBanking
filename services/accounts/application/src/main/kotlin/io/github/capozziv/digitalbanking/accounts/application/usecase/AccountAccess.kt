package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.CallerNotCustomerException
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.security.Caller
import java.util.UUID

/**
 * Who may read an account: its owner, any admin and any service (the transfer saga reads the account to learn its
 * customer). Anyone else gets [AccountNotFoundException], the same as for a missing account, so a customer cannot
 * tell whether the account of another customer exists.
 */
internal fun Caller.requireCanRead(account: Account) {
    val allowed = this is Caller.Admin || this is Caller.Service || actsAsCustomer(account.customerId)
    if (!allowed) throw AccountNotFoundException(account.id)
}

/** The customer the caller acts as when it opens its own account: a customer, or an admin who is also a customer. */
internal fun Caller.ownCustomerId(): UUID =
    when (this) {
        is Caller.Customer -> customerId
        is Caller.Admin -> userId
        Caller.Service -> throw CallerNotCustomerException()
    }
