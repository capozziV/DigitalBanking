package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.CallerNotCustomerException
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
import io.github.capozziv.digitalbanking.accounts.domain.Money
import io.github.capozziv.digitalbanking.accounts.domain.currentBalance
import io.github.capozziv.digitalbanking.security.Caller
import java.time.Clock
import java.time.Instant
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
 * Opens the account and returns its view read in the same transaction. The welcome bonus is per customer: the first
 * account a customer ever opens starts with the bonus credit (sequence 1, recorded in that transaction), so its
 * balance is the bonus. Any later account (the customer closed the earlier one and reopened), or no bonus
 * configured (null), starts at zero with no entry and no bonus event.
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
    private val welcomeBonus: Money?,
    private val clock: Clock,
) {
    fun execute(command: OpenAccountCommand): OpenAccountResult {
        val customerId = command.caller.ownCustomerId() ?: throw CallerNotCustomerException()
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
                    // The welcome bonus is per customer: only the first account ever opened gets it, even if the
                    // earlier one is closed. The insert above already serialized concurrent openings of the customer.
                    val bonusAmount = welcomeBonus.takeUnless { accounts.existsOtherByCustomerId(customerId, account.id) }
                    val balance =
                        bonusAmount?.let { amount ->
                            val bonus =
                                account.credit(
                                    last = null,
                                    entryId = ids.newId(),
                                    amount = amount,
                                    description = WELCOME_BONUS_DESCRIPTION,
                                    idempotencyKey = welcomeBonusKey(account.id),
                                    now = Instant.now(clock),
                                )
                            entries.insert(bonus.entry)
                            outbox.append(bonus.event)
                            bonus.entry.balanceAfter
                        } ?: Balance.ZERO
                    OpenAccountResult(AccountView.of(account, balance, types.findLimit(account.type)), created = true)
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

    companion object {
        const val WELCOME_BONUS_DESCRIPTION = "Bônus de boas-vindas"

        fun welcomeBonusKey(accountId: UUID): String = "welcome-bonus:$accountId"
    }
}
