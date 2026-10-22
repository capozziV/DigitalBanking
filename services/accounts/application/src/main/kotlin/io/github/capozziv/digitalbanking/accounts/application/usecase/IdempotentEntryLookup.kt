package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.IdempotencyKeyReusedException
import io.github.capozziv.digitalbanking.accounts.application.port.LedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.domain.EntryKind
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry

/**
 * Repetition rule shared by credit and debit. Must run with the account lock held, so that no other
 * transaction can record an entry with the same key between this lookup and the insert.
 */
internal class IdempotentEntryLookup(
    private val entries: LedgerEntryRepository,
) {
    /**
     * Returns the original entry when the command repeats it (same key, kind and amount), or null when
     * the key is new for the account. Throws [IdempotencyKeyReusedException] when the key was used for
     * another kind or amount. The description is not compared.
     */
    fun findRepeated(
        command: RecordEntryCommand,
        kind: EntryKind,
    ): LedgerEntry? {
        val existing = entries.findByIdempotencyKey(command.accountId, command.idempotencyKey) ?: return null
        if (existing.kind != kind || existing.amount != command.amount) {
            throw IdempotencyKeyReusedException(command.idempotencyKey)
        }
        return existing
    }
}
