package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.port.AccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeLimit

/** Lists every account type with its overdraft limit, in [AccountType] declaration order. */
class ListAccountTypesUseCase(
    private val types: AccountTypeRepository,
) {
    fun execute(): List<AccountTypeLimit> = types.findAll().sortedBy { it.type }
}
