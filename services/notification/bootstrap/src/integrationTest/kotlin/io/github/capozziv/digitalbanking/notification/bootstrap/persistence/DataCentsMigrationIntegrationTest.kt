package io.github.capozziv.digitalbanking.notification.bootstrap.persistence

import io.github.capozziv.digitalbanking.notification.bootstrap.NotificationIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.util.UUID

/**
 * V3 turns the amounts in cents that older versions wrote as JSON strings into JSON numbers. Runs the migrations
 * of the service in a schema of its own, stopping at V2 to write rows in the old format.
 */
class DataCentsMigrationIntegrationTest {
    private val postgres = NotificationIntegrationTest.postgres
    private val schema = "v3_${UUID.randomUUID().toString().replace("-", "")}"
    private val dataSource = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
    private val jdbc = JdbcClient.create(dataSource)

    private fun flyway(target: String) =
        Flyway
            .configure()
            .dataSource(dataSource)
            .schemas(schema)
            .placeholders(mapOf("appUser" to NotificationIntegrationTest.APP_USER))
            .target(target)
            .load()

    @Test
    fun `string amounts become numbers and the other values stay as they were`() {
        flyway("2").migrate()
        val transfer = insert("""{"transferId":"t-1","amountCents":"12345"}""")
        val typeChanged = insert("""{"accountType":"GOLD","overdraftLimitCents":"200000"}""")
        val welcome = insert("""{"accountId":"a-1","accountType":"FREE"}""")

        val alreadyNumber = insert("""{"transferId":"t-2","amountCents":500}""")

        flyway("latest").migrate()

        // jsonb prints shorter keys first.
        assertThat(data(transfer)).isEqualTo("""{"transferId": "t-1", "amountCents": 12345}""")
        assertThat(data(typeChanged)).isEqualTo("""{"accountType": "GOLD", "overdraftLimitCents": 200000}""")
        assertThat(data(alreadyNumber)).isEqualTo("""{"transferId": "t-2", "amountCents": 500}""")
        assertThat(data(welcome)).isEqualTo("""{"accountId": "a-1", "accountType": "FREE"}""")
    }

    private fun insert(data: String): UUID {
        val id = UUID.randomUUID()
        jdbc
            .sql(
                """
                insert into $schema.notifications (id, customer_id, kind, title, body, data, source_event_id, created_at)
                values (?, ?, 'TRANSFER_SENT', 't', 'b', cast(? as jsonb), ?, now())
                """.trimIndent(),
            ).params(id, UUID.randomUUID(), data, UUID.randomUUID())
            .update()
        return id
    }

    private fun data(id: UUID): String =
        jdbc
            .sql("select data::text from $schema.notifications where id = ?")
            .param(id)
            .query(String::class.java)
            .single()
}
