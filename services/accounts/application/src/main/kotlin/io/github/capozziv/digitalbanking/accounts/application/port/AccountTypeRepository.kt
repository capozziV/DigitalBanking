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

    /** Returns the limit of every account type, read from the database; callers do not rely on its order. */
    fun findAll(): List<AccountTypeLimit>

    /**
     * Sets the overdraft limit and updated_at = [now] of the type in the caller's transaction and returns
     * the stored row. Takes no account lock. Every AccountType has a seeded row, so updating no row is a
     * configuration error and the adapter throws.
     */
    fun updateLimit(
        type: AccountType,
        limit: OverdraftLimit,
        now: Instant,
    ): AccountTypeLimit
}
