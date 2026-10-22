package io.github.capozziv.digitalbanking.accounts.application.port

import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
import java.util.UUID

/**
 * Ledger entries of an account. RecordCreditUseCase and RecordDebitUseCase call every method with the
 * account lock held (AccountRepository.lockById) in the same transaction.
 */
interface LedgerEntryRepository {
    /**
     * Returns the entry with the highest sequence of the account, or null when it has none.
     * Always read from the database, never from a cache: the debit authorizes against its balance.
     */
    fun findLast(accountId: UUID): LedgerEntry?

    /** Returns the entry of the account recorded with this idempotency key; the key is scoped to the account. */
    fun findByIdempotencyKey(
        accountId: UUID,
        key: String,
    ): LedgerEntry?

    /**
     * Inserts the entry. A violation of UNIQUE (account_id, sequence) or UNIQUE (account_id, idempotency_key)
     * is an unexpected error, not a result: the account lock prevents both, so the adapter lets it propagate
     * as an internal error (logged as ERROR) and the transaction rolls back. Callers do not handle it.
     */
    fun insert(entry: LedgerEntry)
}
