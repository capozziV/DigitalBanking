package io.github.capozziv.digitalbanking.statement.application.fake

import io.github.capozziv.digitalbanking.statement.application.port.StatementRepository
import io.github.capozziv.digitalbanking.statement.domain.StatementCursor
import io.github.capozziv.digitalbanking.statement.domain.StatementEntry
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import java.util.UUID

/** Follows the [StatementRepository.findPage] contract the DynamoDB adapter implements. */
class InMemoryStatementRepository : StatementRepository {
    private val entries = mutableMapOf<Pair<UUID, Long>, StatementEntry>()
    private val accounts = mutableMapOf<UUID, UUID>()

    val entryCount: Int get() = entries.size

    override fun insertEntry(entry: StatementEntry): Boolean = entries.putIfAbsent(entry.accountId to entry.sequence, entry) == null

    override fun findEntry(
        accountId: UUID,
        sequence: Long,
    ): StatementEntry? = entries[accountId to sequence]

    override fun insertAccount(
        accountId: UUID,
        customerId: UUID,
    ): Boolean = accounts.putIfAbsent(accountId, customerId) == null

    override fun accountExists(accountId: UUID): Boolean = accountId in accounts

    override fun findCustomerId(accountId: UUID): UUID? = accounts[accountId]

    override fun findPage(
        accountId: UUID,
        cursor: StatementCursor?,
        limit: Int,
    ): StatementPage {
        val page =
            entries.values
                .filter { it.accountId == accountId && (cursor == null || it.sequence < cursor.beforeSequence) }
                .sortedByDescending { it.sequence }
                .take(limit)
        val last = page.lastOrNull()
        val nextCursor = if (page.size == limit && last != null && last.sequence > 1) StatementCursor(last.sequence) else null
        return StatementPage(page, nextCursor)
    }
}
