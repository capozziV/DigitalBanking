package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.CustomerAlreadyHasAccountException
import io.github.capozziv.digitalbanking.accounts.application.IdempotencyKeyReusedException
import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.AccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.port.EventOutbox
import io.github.capozziv.digitalbanking.accounts.application.port.IdGenerator
import io.github.capozziv.digitalbanking.accounts.application.port.InsertAccountResult
import io.github.capozziv.digitalbanking.accounts.application.port.LedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.Balance
import io.github.capozziv.digitalbanking.accounts.domain.currentBalance
import java.util.UUID

data class OpenAccountCommand(
    val customerId: UUID,
    val idempotencyKey: String,
)

data class OpenAccountResult(
    val view: AccountView,
    val created: Boolean,
)

/** Opens the account and returns its view read in the same transaction; a new account has no entries, so its balance is zero. */
class OpenAccountUseCase(
    private val accounts: AccountRepository,
    private val entries: LedgerEntryRepository,
    private val types: AccountTypeRepository,
    private val outbox: EventOutbox,
    private val tx: TransactionRunner,
    private val ids: IdGenerator,
) {
    fun execute(command: OpenAccountCommand): OpenAccountResult =
        tx.inTransaction {
            val (account, event) = Account.open(ids.newId(), command.customerId)
            when (accounts.insert(account, command.idempotencyKey)) {
                InsertAccountResult.INSERTED -> {
                    outbox.append(event)
                    OpenAccountResult(AccountView.of(account, Balance.ZERO, types.findLimit(account.type)), created = true)
                }

                InsertAccountResult.DUPLICATE_OPENING_KEY -> {
                    val existing =
                        checkNotNull(accounts.findByOpeningIdempotencyKey(command.idempotencyKey)) {
                            "account with opening key ${command.idempotencyKey} not found after a duplicate key conflict"
                        }
                    if (existing.customerId != command.customerId) {
                        throw IdempotencyKeyReusedException(command.idempotencyKey)
                    }
                    val view = AccountView.of(existing, entries.findLast(existing.id).currentBalance(), types.findLimit(existing.type))
                    OpenAccountResult(view, created = false)
                }

                InsertAccountResult.DUPLICATE_CUSTOMER -> {
                    throw CustomerAlreadyHasAccountException(command.customerId)
                }
            }
        }
}
