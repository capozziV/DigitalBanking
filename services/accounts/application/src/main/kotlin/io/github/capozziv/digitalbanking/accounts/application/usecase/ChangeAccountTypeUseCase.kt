package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.EventOutbox
import io.github.capozziv.digitalbanking.accounts.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import java.util.UUID

/**
 * Changes the account type under the account lock. The same type returns the account unchanged, without
 * update or event. A downgrade is accepted even when the balance is below the new overdraft limit: the
 * balance is not read here.
 */
class ChangeAccountTypeUseCase(
    private val accounts: AccountRepository,
    private val outbox: EventOutbox,
    private val tx: TransactionRunner,
) {
    fun execute(
        accountId: UUID,
        newType: AccountType,
    ): Account =
        tx.inTransaction {
            val account = accounts.lockById(accountId) ?: throw AccountNotFoundException(accountId)
            val (changed, event) = account.changeType(newType) ?: return@inTransaction account
            accounts.updateType(changed)
            outbox.append(event)
            changed
        }
}
