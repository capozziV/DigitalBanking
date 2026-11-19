package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.NoOpenAccountException
import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.TransactionRunner
import java.util.UUID

/** Finds where a transfer to a customer lands: the id of its account that is not closed, and nothing else. */
class FindRecipientAccountUseCase(
    private val accounts: AccountRepository,
    private val tx: TransactionRunner,
) {
    /** Fails with [NoOpenAccountException] when the customer has no account that is not closed. */
    fun execute(customerId: UUID): UUID =
        tx.inReadOnlyTransaction {
            (accounts.findNotClosedByCustomerId(customerId) ?: throw NoOpenAccountException(customerId)).id
        }
}
