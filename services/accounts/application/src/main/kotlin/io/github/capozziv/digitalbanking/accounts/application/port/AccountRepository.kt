package io.github.capozziv.digitalbanking.accounts.application.port

import io.github.capozziv.digitalbanking.accounts.domain.Account
import java.util.UUID

interface AccountRepository {
    /**
     * Inserts a new account with its opening idempotency key.
     *
     * Contract relied on by OpenAccountUseCase:
     * - a duplicate (opening key or customer) is reported through the result and must NOT abort the
     *   current transaction: the caller re-reads the account in the same transaction;
     * - when the opening key and the customer collide at the same time, DUPLICATE_OPENING_KEY takes
     *   precedence over DUPLICATE_CUSTOMER.
     */
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
