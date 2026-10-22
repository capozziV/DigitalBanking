package io.github.capozziv.digitalbanking.accounts.application.port

import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeLimit
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import java.time.Instant

interface AccountTypeRepository {
    fun findLimit(type: AccountType): OverdraftLimit

    fun findAll(): List<AccountTypeLimit>

    fun updateLimit(
        type: AccountType,
        limit: OverdraftLimit,
        now: Instant,
    ): AccountTypeLimit
}
