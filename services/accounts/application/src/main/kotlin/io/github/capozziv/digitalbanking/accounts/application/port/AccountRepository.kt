package io.github.capozziv.digitalbanking.accounts.application.port

import io.github.capozziv.digitalbanking.accounts.domain.Account
import java.util.UUID

interface AccountRepository {
    fun insert(
        account: Account,
        openingIdempotencyKey: String,
    ): InsertAccountResult

    fun findById(id: UUID): Account?

    /** Locks the account row (FOR UPDATE with lock_timeout); throws AccountBusyException when the lock times out. */
    fun lockById(id: UUID): Account?

    fun findByOpeningIdempotencyKey(key: String): Account?

    fun updateType(account: Account)
}

enum class InsertAccountResult { INSERTED, DUPLICATE_OPENING_KEY, DUPLICATE_CUSTOMER }
