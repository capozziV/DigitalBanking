package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.CustomerAlreadyHasAccountException
import io.github.capozziv.digitalbanking.accounts.application.CustomerBlockedException
import io.github.capozziv.digitalbanking.accounts.application.CustomerNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.IdempotencyKeyReusedException
import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.AccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.port.CustomerGateway
import io.github.capozziv.digitalbanking.accounts.application.port.CustomerStatus
import io.github.capozziv.digitalbanking.accounts.application.port.EventOutbox
import io.github.capozziv.digitalbanking.accounts.application.port.IdGenerator
import io.github.capozziv.digitalbanking.accounts.application.port.InsertAccountResult
import io.github.capozziv.digitalbanking.accounts.application.port.LedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.Balance
import io.github.capozziv.digitalbanking.accounts.domain.currentBalance
import io.github.capozziv.digitalbanking.security.Caller
import java.util.UUID

/** Opens the account of the [caller] itself: its customer id comes from the token, never from the request body. */
data class OpenAccountCommand(
    val caller: Caller,
    val idempotencyKey: String,
)

data class OpenAccountResult(
    val view: AccountView,
    val created: Boolean,
)

/**
 * Opens the account and returns its view read in the same transaction; a new account has no entries, so its balance is zero.
 *
 * First asks the `customer` service, outside the transaction (no connection held during the call), whether the
 * customer exists and is active: [CustomerNotFoundException], [CustomerBlockedException] or
 * [io.github.capozziv.digitalbanking.accounts.application.CustomersUnavailableException] write nothing.
 * A retry with the same opening key asks again, so a customer blocked meanwhile is refused.
 */
class OpenAccountUseCase(
    private val accounts: AccountRepository,
    private val entries: LedgerEntryRepository,
    private val types: AccountTypeRepository,
    private val outbox: EventOutbox,
    private val tx: TransactionRunner,
    private val ids: IdGenerator,
    private val customers: CustomerGateway,
) {
    fun execute(command: OpenAccountCommand): OpenAccountResult {
        val customerId = command.caller.ownCustomerId()
        when (customers.findStatus(customerId)) {
            null -> throw CustomerNotFoundException(customerId)
            CustomerStatus.BLOCKED -> throw CustomerBlockedException(customerId)
            CustomerStatus.ACTIVE -> Unit
        }
        return open(customerId, command.idempotencyKey)
    }

    private fun open(
        customerId: UUID,
        idempotencyKey: String,
    ): OpenAccountResult =
        tx.inTransaction {
            val (account, event) = Account.open(ids.newId(), customerId)
            when (accounts.insert(account, idempotencyKey)) {
                InsertAccountResult.INSERTED -> {
                    outbox.append(event)
                    OpenAccountResult(AccountView.of(account, Balance.ZERO, types.findLimit(account.type)), created = true)
                }

                InsertAccountResult.DUPLICATE_OPENING_KEY -> {
                    val existing =
                        checkNotNull(accounts.findByOpeningIdempotencyKey(idempotencyKey)) {
                            "account with opening key $idempotencyKey not found after a duplicate key conflict"
                        }
                    if (existing.customerId != customerId) {
                        throw IdempotencyKeyReusedException(idempotencyKey)
                    }
                    val view = AccountView.of(existing, entries.findLast(existing.id).currentBalance(), types.findLimit(existing.type))
                    OpenAccountResult(view, created = false)
                }

                InsertAccountResult.DUPLICATE_CUSTOMER -> {
                    throw CustomerAlreadyHasAccountException(customerId)
                }
            }
        }
}
