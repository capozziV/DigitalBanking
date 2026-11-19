package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.CallerNotCustomerException
import io.github.capozziv.digitalbanking.accounts.application.NoOpenAccountException
import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.AccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.port.LedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.accounts.domain.currentBalance
import io.github.capozziv.digitalbanking.security.Caller

/** Reads the account of the caller (the one not closed), found by the customer of the token, with balance and limit. */
class GetOwnAccountUseCase(
    private val accounts: AccountRepository,
    private val entries: LedgerEntryRepository,
    private val types: AccountTypeRepository,
    private val tx: TransactionRunner,
) {
    /** Fails with [CallerNotCustomerException] for a service and [NoOpenAccountException] when there is no account. */
    fun execute(caller: Caller): AccountView {
        val customerId = caller.ownCustomerId() ?: throw CallerNotCustomerException()
        return tx.inReadOnlyTransaction {
            val account = accounts.findNotClosedByCustomerId(customerId) ?: throw NoOpenAccountException(customerId)
            AccountView.of(account, entries.findLast(account.id).currentBalance(), types.findLimit(account.type))
        }
    }
}
