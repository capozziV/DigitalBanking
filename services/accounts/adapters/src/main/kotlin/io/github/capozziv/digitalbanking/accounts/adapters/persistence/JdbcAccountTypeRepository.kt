package io.github.capozziv.digitalbanking.accounts.adapters.persistence

import io.github.capozziv.digitalbanking.accounts.application.port.AccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeLimit
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class JdbcAccountTypeRepository(
    private val jdbcClient: JdbcClient,
) : AccountTypeRepository {
    override fun findLimit(type: AccountType): OverdraftLimit =
        jdbcClient
            .sql("select overdraft_limit from account_type where type = :type")
            .param("type", type.name)
            .query(Long::class.java)
            .optional()
            .map { OverdraftLimit(it) }
            .orElseThrow { IllegalStateException("account type $type has no row in account_type") }

    override fun findAll(): List<AccountTypeLimit> =
        jdbcClient
            .sql("select type, overdraft_limit, updated_at from account_type")
            .query(LIMIT_ROW_MAPPER)
            .list()

    override fun updateLimit(
        type: AccountType,
        limit: OverdraftLimit,
        now: Instant,
    ): AccountTypeLimit =
        jdbcClient
            .sql(
                """
                update account_type set overdraft_limit = :limit, updated_at = :now
                where type = :type
                returning type, overdraft_limit, updated_at
                """.trimIndent(),
            ).param("limit", limit.cents)
            .param("now", OffsetDateTime.ofInstant(now, ZoneOffset.UTC))
            .param("type", type.name)
            .query(LIMIT_ROW_MAPPER)
            .optional()
            .orElseThrow { IllegalStateException("account type $type has no row in account_type") }

    private companion object {
        val LIMIT_ROW_MAPPER =
            RowMapper { rs, _ ->
                AccountTypeLimit(
                    type = AccountType.valueOf(rs.getString("type")),
                    overdraftLimit = OverdraftLimit(rs.getLong("overdraft_limit")),
                    updatedAt = rs.getObject("updated_at", OffsetDateTime::class.java).toInstant(),
                )
            }
    }
}
