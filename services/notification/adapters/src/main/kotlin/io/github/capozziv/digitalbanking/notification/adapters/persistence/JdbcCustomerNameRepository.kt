package io.github.capozziv.digitalbanking.notification.adapters.persistence

import io.github.capozziv.digitalbanking.notification.application.port.CustomerNameRepository
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JdbcCustomerNameRepository(
    private val jdbcClient: JdbcClient,
) : CustomerNameRepository {
    override fun save(
        customerId: UUID,
        name: String,
    ) {
        jdbcClient
            .sql(
                """
                insert into customer_names (customer_id, name)
                values (:customerId, :name)
                on conflict (customer_id) do update set name = excluded.name
                """.trimIndent(),
            ).param("customerId", customerId)
            .param("name", name)
            .update()
    }

    override fun namesOfAccountOwners(accountIds: Set<UUID>): Map<UUID, String> {
        if (accountIds.isEmpty()) return emptyMap()
        return jdbcClient
            .sql(
                """
                select a.account_id, n.name
                from known_accounts a
                join customer_names n on n.customer_id = a.customer_id
                where a.account_id in (:accountIds)
                """.trimIndent(),
            ).param("accountIds", accountIds.toList())
            .query { rs, _ -> rs.getObject("account_id", UUID::class.java) to rs.getString("name") }
            .list()
            .toMap()
    }
}
