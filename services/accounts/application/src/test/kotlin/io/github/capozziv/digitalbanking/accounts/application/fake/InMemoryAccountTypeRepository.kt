package io.github.capozziv.digitalbanking.accounts.application.fake

import io.github.capozziv.digitalbanking.accounts.application.port.AccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeLimit
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import java.time.Instant

class InMemoryAccountTypeRepository(
    initial: List<AccountTypeLimit>,
) : AccountTypeRepository {
    private val limits = initial.associateBy { it.type }.toMutableMap()

    override fun findLimit(type: AccountType): OverdraftLimit = limits.getValue(type).overdraftLimit

    override fun findAll(): List<AccountTypeLimit> = limits.values.sortedBy { it.type }

    override fun updateLimit(
        type: AccountType,
        limit: OverdraftLimit,
        now: Instant,
    ): AccountTypeLimit {
        val updated = AccountTypeLimit(type, limit, now)
        limits[type] = updated
        return updated
    }
}
