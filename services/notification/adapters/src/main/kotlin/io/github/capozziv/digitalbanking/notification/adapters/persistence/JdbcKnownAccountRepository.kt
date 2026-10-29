package io.github.capozziv.digitalbanking.notification.adapters.persistence

import io.github.capozziv.digitalbanking.notification.application.port.KnownAccountRepository
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JdbcKnownAccountRepository(
    private val jdbcClient: JdbcClient,
) : KnownAccountRepository {
    override fun save(
        accountId: UUID,
        customerId: UUID,
    ) {
        jdbcClient
            .sql(
                """
                insert into known_accounts (account_id, customer_id)
                values (:accountId, :customerId)
                on conflict (account_id) do nothing
                """.trimIndent(),
            ).param("accountId", accountId)
            .param("customerId", customerId)
            .update()
    }

    override fun customerOf(accountId: UUID): UUID? =
        jdbcClient
            .sql("select customer_id from known_accounts where account_id = :accountId")
            .param("accountId", accountId)
            .query(UUID::class.java)
            .optional()
            .orElse(null)
}
