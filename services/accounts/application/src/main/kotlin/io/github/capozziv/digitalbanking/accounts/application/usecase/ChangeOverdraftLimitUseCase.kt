package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.port.AccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeLimit
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import java.time.Clock
import java.time.Instant

/**
 * Sets the overdraft limit of an account type. No event and no account lock: debits in progress keep
 * the limit they already read.
 */
class ChangeOverdraftLimitUseCase(
    private val types: AccountTypeRepository,
    private val tx: TransactionRunner,
    private val clock: Clock,
) {
    fun execute(
        type: AccountType,
        limit: OverdraftLimit,
    ): AccountTypeLimit = tx.inTransaction { types.updateLimit(type, limit, Instant.now(clock)) }
}
