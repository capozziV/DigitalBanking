package io.github.capozziv.digitalbanking.accounts.adapters.persistence

import io.github.capozziv.digitalbanking.accounts.application.AccountBusyException
import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.InsertAccountResult
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountStatus
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.SQLException
import java.time.Duration
import java.util.UUID

@Repository
class JdbcAccountRepository(
    private val jdbcClient: JdbcClient,
    @param:Value("\${accounts.lock-timeout:2s}") private val lockTimeout: Duration,
) : AccountRepository {
    /**
     * ON CONFLICT DO NOTHING (no target) so that no unique violation (opening key, or the partial index of one account
     * not closed per customer) aborts the caller's transaction.
     * With no row inserted, the opening key decides the result: it takes precedence over the customer.
     * A concurrent uncommitted insert with the same key makes this statement wait for its outcome.
     */
    override fun insert(
        account: Account,
        openingIdempotencyKey: String,
    ): InsertAccountResult {
        val inserted =
            jdbcClient
                .sql(
                    """
                    insert into account (id, customer_id, type, status, opening_idempotency_key)
                    values (:id, :customerId, :type, :status, :openingKey)
                    on conflict do nothing
                    """.trimIndent(),
                ).param("id", account.id)
                .param("customerId", account.customerId)
                .param("type", account.type.name)
                .param("status", account.status.name)
                .param("openingKey", openingIdempotencyKey)
                .update()
        return when {
            inserted == 1 -> InsertAccountResult.INSERTED
            findByOpeningIdempotencyKey(openingIdempotencyKey) != null -> InsertAccountResult.DUPLICATE_OPENING_KEY
            else -> InsertAccountResult.DUPLICATE_CUSTOMER
        }
    }

    override fun findById(id: UUID): Account? =
        jdbcClient
            .sql("select id, customer_id, type, status from account where id = :id")
            .param("id", id)
            .query(ACCOUNT_ROW_MAPPER)
            .optional()
            .orElse(null)

    override fun findNotClosedByCustomerId(customerId: UUID): Account? =
        jdbcClient
            .sql("select id, customer_id, type, status from account where customer_id = :customerId and status <> 'CLOSED'")
            .param("customerId", customerId)
            .query(ACCOUNT_ROW_MAPPER)
            .optional()
            .orElse(null)

    override fun lockById(id: UUID): Account? {
        // SET does not take bind parameters; the value is a number of milliseconds, never user input.
        jdbcClient.sql("set local lock_timeout = '${lockTimeout.toMillis()}ms'").update()
        return try {
            jdbcClient
                .sql("select id, customer_id, type, status from account where id = :id for update")
                .param("id", id)
                .query(ACCOUNT_ROW_MAPPER)
                .optional()
                .orElse(null)
        } catch (e: DataAccessException) {
            if (e.sqlState() == LOCK_NOT_AVAILABLE) throw AccountBusyException(id)
            throw e
        }
    }

    override fun findByOpeningIdempotencyKey(key: String): Account? =
        jdbcClient
            .sql("select id, customer_id, type, status from account where opening_idempotency_key = :key")
            .param("key", key)
            .query(ACCOUNT_ROW_MAPPER)
            .optional()
            .orElse(null)

    override fun updateType(account: Account) {
        val updated =
            jdbcClient
                .sql("update account set type = :type, updated_at = now() where id = :id")
                .param("type", account.type.name)
                .param("id", account.id)
                .update()
        check(updated == 1) { "account ${account.id} was not updated" }
    }

    override fun updateStatus(account: Account) {
        val updated =
            jdbcClient
                .sql("update account set status = :status, updated_at = now() where id = :id")
                .param("status", account.status.name)
                .param("id", account.id)
                .update()
        check(updated == 1) { "account ${account.id} was not updated" }
    }

    private fun DataAccessException.sqlState(): String? =
        generateSequence<Throwable>(this) { it.cause }.filterIsInstance<SQLException>().firstOrNull()?.sqlState

    private companion object {
        const val LOCK_NOT_AVAILABLE = "55P03"

        val ACCOUNT_ROW_MAPPER =
            RowMapper { rs, _ ->
                Account(
                    id = rs.getObject("id", UUID::class.java),
                    customerId = rs.getObject("customer_id", UUID::class.java),
                    type = AccountType.valueOf(rs.getString("type")),
                    status = AccountStatus.valueOf(rs.getString("status")),
                )
            }
    }
}
