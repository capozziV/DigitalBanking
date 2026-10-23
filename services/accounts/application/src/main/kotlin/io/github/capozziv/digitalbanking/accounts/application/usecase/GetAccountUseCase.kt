package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.AccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.port.LedgerEntryRepository
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
)

class GetAccountUseCase(
    private val accounts: AccountRepository,
    private val entries: LedgerEntryRepository,
    private val types: AccountTypeRepository,
) {
    fun execute(accountId: UUID): AccountView {
        val account = accounts.findById(accountId) ?: throw AccountNotFoundException(accountId)
        val balance = entries.findLast(accountId).currentBalance()
        val limit = types.findLimit(account.type)
        return AccountView(account, balance, limit, AvailableBalance.of(balance, limit))
    }
}
