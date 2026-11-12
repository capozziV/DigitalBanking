package io.github.capozziv.digitalbanking.statement.application.port

import io.github.capozziv.digitalbanking.statement.domain.StatementCursor
import io.github.capozziv.digitalbanking.statement.domain.StatementEntry
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import java.util.UUID

interface StatementRepository {
    /** Stores the entry only if no entry exists for its account and sequence; false means the key already exists. */
    fun insertEntry(entry: StatementEntry): Boolean

    /**
     * The entry stored for the account and sequence, or null. The read must be strongly consistent: the record use
     * case reads right after [insertEntry] was rejected and must see the item that caused the rejection.
     */
    fun findEntry(
        accountId: UUID,
        sequence: Long,
    ): StatementEntry?

    /** Stores the account only if it is not stored yet; false means it already existed. */
    fun insertAccount(
        accountId: UUID,
        customerId: UUID,
    ): Boolean

    fun accountExists(accountId: UUID): Boolean

    /**
     * Entries of the account with a sequence lower than [cursor] (all of them without a cursor), newest first,
     * at most [limit]. The next cursor is the last returned sequence when the page is full and that sequence
     * is above 1; otherwise it is null. A cursor of 1 is past the oldest entry: the page is empty, with no cursor.
     */
    fun findPage(
        accountId: UUID,
        cursor: StatementCursor?,
        limit: Int,
    ): StatementPage
}
