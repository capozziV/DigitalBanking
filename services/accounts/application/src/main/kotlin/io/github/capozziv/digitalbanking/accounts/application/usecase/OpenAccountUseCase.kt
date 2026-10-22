package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.CustomerAlreadyHasAccountException
import io.github.capozziv.digitalbanking.accounts.application.IdempotencyKeyReusedException
import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.EventOutbox
import io.github.capozziv.digitalbanking.accounts.application.port.IdGenerator
import io.github.capozziv.digitalbanking.accounts.application.port.InsertAccountResult
import io.github.capozziv.digitalbanking.accounts.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.accounts.domain.Account
import java.util.UUID

data class OpenAccountCommand(
    val customerId: UUID,
    val idempotencyKey: String,
)

data class OpenAccountResult(
    val account: Account,
    val created: Boolean,
)

class OpenAccountUseCase(
    private val accounts: AccountRepository,
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
                    OpenAccountResult(account, created = true)
                }

                InsertAccountResult.DUPLICATE_OPENING_KEY -> {
                    val existing =
                        checkNotNull(accounts.findByOpeningIdempotencyKey(command.idempotencyKey)) {
                            "account with opening key ${command.idempotencyKey} not found after a duplicate key conflict"
                        }
                    if (existing.customerId != command.customerId) {
                        throw IdempotencyKeyReusedException(command.idempotencyKey)
                    }
                    OpenAccountResult(existing, created = false)
                }

                InsertAccountResult.DUPLICATE_CUSTOMER -> {
                    throw CustomerAlreadyHasAccountException(command.customerId)
                }
            }
        }
}
