package io.github.capozziv.digitalbanking.accounts.application.port

import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeLimit
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import java.time.Instant

interface AccountTypeRepository {
    /**
     * Returns the current overdraft limit of the type, read from the database. Every AccountType has a
     * row seeded by migration, so a missing row is a configuration error and the adapter throws.
     */
    fun findLimit(type: AccountType): OverdraftLimit

    fun findAll(): List<AccountTypeLimit>

    fun updateLimit(
        type: AccountType,
        limit: OverdraftLimit,
        now: Instant,
    ): AccountTypeLimit
}
