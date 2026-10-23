package io.github.capozziv.digitalbanking.accounts.adapters.persistence

import io.github.capozziv.digitalbanking.accounts.application.port.LedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.domain.Balance
import io.github.capozziv.digitalbanking.accounts.domain.EntryKind
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
import io.github.capozziv.digitalbanking.accounts.domain.Money
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
class JdbcLedgerEntryRepository(
    private val jdbcClient: JdbcClient,
) : LedgerEntryRepository {
    override fun findLast(accountId: UUID): LedgerEntry? =
        jdbcClient
            .sql("select $COLUMNS from ledger_entry where account_id = :accountId order by sequence desc limit 1")
            .param("accountId", accountId)
            .query(ENTRY_ROW_MAPPER)
            .optional()
            .orElse(null)

    override fun findByIdempotencyKey(
        accountId: UUID,
        key: String,
    ): LedgerEntry? =
        jdbcClient
            .sql("select $COLUMNS from ledger_entry where account_id = :accountId and idempotency_key = :key")
            .param("accountId", accountId)
            .param("key", key)
            .query(ENTRY_ROW_MAPPER)
            .optional()
            .orElse(null)

    override fun insert(entry: LedgerEntry) {
        // timestamptz keeps microseconds: a finer createdAt would come back different from the entry and its event.
        require(entry.createdAt.nano % 1_000 == 0) { "createdAt must have microsecond precision" }
        try {
            jdbcClient
                .sql(
                    """
                    insert into ledger_entry ($COLUMNS)
                    values (:id, :accountId, :sequence, :kind, :amount, :balanceAfter, :description, :idempotencyKey, :createdAt)
                    """.trimIndent(),
                ).param("id", entry.id)
                .param("accountId", entry.accountId)
                .param("sequence", entry.sequence)
                .param("kind", entry.kind.name)
                .param("amount", entry.amount.cents)
                .param("balanceAfter", entry.balanceAfter.cents)
                .param("description", entry.description)
                .param("idempotencyKey", entry.idempotencyKey)
                .param("createdAt", OffsetDateTime.ofInstant(entry.createdAt, ZoneOffset.UTC))
                .update()
        } catch (e: DuplicateKeyException) {
            // The account lock should make this impossible: it signals a bug, so it is not handled as a result.
            log.error("unexpected duplicate ledger entry for account {} (sequence {})", entry.accountId, entry.sequence, e)
            throw e
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(JdbcLedgerEntryRepository::class.java)

        const val COLUMNS = "id, account_id, sequence, kind, amount, balance_after, description, idempotency_key, created_at"

        val ENTRY_ROW_MAPPER =
            RowMapper { rs, _ ->
                LedgerEntry(
                    id = rs.getObject("id", UUID::class.java),
                    accountId = rs.getObject("account_id", UUID::class.java),
                    sequence = rs.getLong("sequence"),
                    kind = EntryKind.valueOf(rs.getString("kind")),
                    amount = Money(rs.getLong("amount")),
                    balanceAfter = Balance(rs.getLong("balance_after")),
                    description = rs.getString("description"),
                    idempotencyKey = rs.getString("idempotency_key"),
                    createdAt = rs.getObject("created_at", OffsetDateTime::class.java).toInstant(),
                )
            }
    }
}
