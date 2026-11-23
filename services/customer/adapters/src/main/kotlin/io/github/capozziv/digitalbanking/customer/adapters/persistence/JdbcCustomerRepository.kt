package io.github.capozziv.digitalbanking.customer.adapters.persistence

import io.github.capozziv.digitalbanking.customer.application.PhoneAlreadyInUseException
import io.github.capozziv.digitalbanking.customer.application.port.CustomerRepository
import io.github.capozziv.digitalbanking.customer.domain.Customer
import io.github.capozziv.digitalbanking.customer.domain.CustomerStatus
import io.github.capozziv.digitalbanking.customer.domain.PhoneNumber
import org.springframework.dao.DuplicateKeyException
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
        refusingDuplicatePhone(customer.phone) {
            jdbcClient
                .sql(
                    """
                    insert into customer (id, name, email, phone, status, created_at, updated_at)
                    values (:id, :name, :email, :phone, :status, :createdAt, :updatedAt)
                    on conflict (id) do nothing
                    """.trimIndent(),
                ).param("id", customer.id)
                .param("name", customer.name)
                .param("email", customer.email)
                .param("phone", customer.phone.value)
                .param("status", customer.status.name)
                .param("createdAt", customer.createdAt.atOffset(java.time.ZoneOffset.UTC))
                .param("updatedAt", customer.updatedAt.atOffset(java.time.ZoneOffset.UTC))
                .update() == 1
        }

    override fun findById(id: UUID): Customer? = select("select $COLUMNS from customer where id = :id", id)

    override fun lockById(id: UUID): Customer? = select("select $COLUMNS from customer where id = :id for update", id)

    override fun findByPhone(phone: PhoneNumber): Customer? =
        jdbcClient
            .sql("select $COLUMNS from customer where phone = :phone")
            .param("phone", phone.value)
            .query(CUSTOMER_ROW_MAPPER)
            .optional()
            .orElse(null)

    override fun update(customer: Customer) {
        val updated =
            refusingDuplicatePhone(customer.phone) {
                jdbcClient
                    .sql("update customer set status = :status, phone = :phone, updated_at = :updatedAt where id = :id")
                    .param("status", customer.status.name)
                    .param("phone", customer.phone.value)
                    .param("updatedAt", customer.updatedAt.atOffset(java.time.ZoneOffset.UTC))
                    .param("id", customer.id)
                    .update()
            }
        check(updated == 1) { "customer ${customer.id} was not updated" }
    }

    /**
     * The unique index on the phone is what refuses a number another customer has, also for two concurrent writes.
     *
     * Note: migration V4 gave the customers that existed then fictitious phones (+5511900000001 onwards). A real number
     * chosen later may equal one of them and is refused like any other duplicate (409). The note lives here, not in
     * V4, because editing an applied migration changes its Flyway checksum.
     */
    private fun <T> refusingDuplicatePhone(
        phone: PhoneNumber,
        write: () -> T,
    ): T =
        try {
            write()
        } catch (e: DuplicateKeyException) {
            throw PhoneAlreadyInUseException(phone).also { it.initCause(e) }
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
        const val COLUMNS = "id, name, email, phone, status, created_at, updated_at"

        val CUSTOMER_ROW_MAPPER =
            RowMapper { rs, _ ->
                Customer(
                    id = rs.getObject("id", UUID::class.java),
                    name = rs.getString("name"),
                    email = rs.getString("email"),
                    phone = PhoneNumber(rs.getString("phone")),
                    status = CustomerStatus.valueOf(rs.getString("status")),
                    createdAt = rs.getObject("created_at", java.time.OffsetDateTime::class.java).toInstant(),
                    updatedAt = rs.getObject("updated_at", java.time.OffsetDateTime::class.java).toInstant(),
                )
            }
    }
}
