package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.AccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.port.LedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AvailableBalance
import io.github.capozziv.digitalbanking.accounts.domain.Balance
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import io.github.capozziv.digitalbanking.accounts.domain.currentBalance
import java.util.UUID

data class AccountView(
    val account: Account,
    val balance: Balance,
    val overdraftLimit: OverdraftLimit,
    val availableBalance: AvailableBalance,
) {
    companion object {
        fun of(
            account: Account,
            balance: Balance,
            overdraftLimit: OverdraftLimit,
        ) = AccountView(account, balance, overdraftLimit, AvailableBalance.of(balance, overdraftLimit))
    }
}

/** Reads the account, its balance and its overdraft limit in one read-only transaction, so the three agree. */
class GetAccountUseCase(
    private val accounts: AccountRepository,
    private val entries: LedgerEntryRepository,
    private val types: AccountTypeRepository,
    private val tx: TransactionRunner,
) {
    fun execute(accountId: UUID): AccountView =
        tx.inReadOnlyTransaction {
            val account = accounts.findById(accountId) ?: throw AccountNotFoundException(accountId)
            AccountView.of(account, entries.findLast(accountId).currentBalance(), types.findLimit(account.type))
        }
}
