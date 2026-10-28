package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.LedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
import java.util.UUID

/**
 * Read-only lookup of the entry recorded on an account with an idempotency key. It lets a caller that
 * lost the response of a credit or debit find out whether it happened. Takes no lock and writes nothing.
 */
class FindEntryByIdempotencyKeyUseCase(
    private val accounts: AccountRepository,
    private val entries: LedgerEntryRepository,
) {
    /** The entry, or null when the key was never recorded on this account. */
    fun execute(
        accountId: UUID,
        key: String,
    ): LedgerEntry? {
        accounts.findById(accountId) ?: throw AccountNotFoundException(accountId)
        return entries.findByIdempotencyKey(accountId, key)
    }
}
