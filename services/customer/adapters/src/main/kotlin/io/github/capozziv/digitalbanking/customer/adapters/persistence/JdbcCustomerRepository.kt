package io.github.capozziv.digitalbanking.customer.adapters.persistence

import io.github.capozziv.digitalbanking.customer.application.port.CustomerRepository
import io.github.capozziv.digitalbanking.customer.domain.Customer
import io.github.capozziv.digitalbanking.customer.domain.CustomerStatus
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class JdbcCustomerRepository(
    private val jdbcClient: JdbcClient,
) : CustomerRepository {
    /**
     * ON CONFLICT DO NOTHING, so a second registration of the same user does not abort the caller's transaction.
     * A concurrent uncommitted insert of the same id makes this statement wait for its outcome.
     */
    override fun insertIfAbsent(customer: Customer): Boolean =
        jdbcClient
            .sql(
                """
                insert into customer (id, name, email, status, created_at, updated_at)
                values (:id, :name, :email, :status, :createdAt, :updatedAt)
                on conflict (id) do nothing
                """.trimIndent(),
            ).param("id", customer.id)
            .param("name", customer.name)
            .param("email", customer.email)
            .param("status", customer.status.name)
            .param("createdAt", customer.createdAt.atOffset(java.time.ZoneOffset.UTC))
            .param("updatedAt", customer.updatedAt.atOffset(java.time.ZoneOffset.UTC))
            .update() == 1

    override fun findById(id: UUID): Customer? = select("select $COLUMNS from customer where id = :id", id)

    override fun lockById(id: UUID): Customer? = select("select $COLUMNS from customer where id = :id for update", id)

    override fun update(customer: Customer) {
        val updated =
            jdbcClient
                .sql("update customer set status = :status, updated_at = :updatedAt where id = :id")
                .param("status", customer.status.name)
                .param("updatedAt", customer.updatedAt.atOffset(java.time.ZoneOffset.UTC))
                .param("id", customer.id)
                .update()
        check(updated == 1) { "customer ${customer.id} was not updated" }
    }

    private fun select(
        sql: String,
        id: UUID,
    ): Customer? =
        jdbcClient
            .sql(sql)
            .param("id", id)
            .query(CUSTOMER_ROW_MAPPER)
            .optional()
            .orElse(null)

    private companion object {
        const val COLUMNS = "id, name, email, status, created_at, updated_at"

        val CUSTOMER_ROW_MAPPER =
            RowMapper { rs, _ ->
                Customer(
                    id = rs.getObject("id", UUID::class.java),
                    name = rs.getString("name"),
                    email = rs.getString("email"),
                    status = CustomerStatus.valueOf(rs.getString("status")),
                    createdAt = rs.getObject("created_at", java.time.OffsetDateTime::class.java).toInstant(),
                    updatedAt = rs.getObject("updated_at", java.time.OffsetDateTime::class.java).toInstant(),
                )
            }
    }
}
