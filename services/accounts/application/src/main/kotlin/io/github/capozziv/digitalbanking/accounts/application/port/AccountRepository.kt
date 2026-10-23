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

    /**
     * Locks the account row until the end of the current transaction (SELECT ... FOR UPDATE with the
     * configured lock_timeout). Returns null when the account does not exist. Throws AccountBusyException
     * when the lock times out; the transaction is then aborted and nothing must be written.
     * Credit and debit rely on this lock to serialize entries of the same account.
     */
    fun lockById(id: UUID): Account?

    fun findByOpeningIdempotencyKey(key: String): Account?

    /**
     * Writes the type of an existing account. ChangeAccountTypeUseCase calls it with the account lock held
     * (lockById) in the same transaction, always with an account produced by Account.changeType. Only the
     * type column changes; the account exists because it is locked, so updating no row is an unexpected
     * error and the adapter throws (the transaction rolls back).
     */
    fun updateType(account: Account)
}

enum class InsertAccountResult { INSERTED, DUPLICATE_OPENING_KEY, DUPLICATE_CUSTOMER }
