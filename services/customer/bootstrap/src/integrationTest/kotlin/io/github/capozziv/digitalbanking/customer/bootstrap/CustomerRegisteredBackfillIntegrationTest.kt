package io.github.capozziv.digitalbanking.customer.bootstrap

import io.github.capozziv.digitalbanking.customer.bootstrap.web.ApiClient
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

/**
 * V5 republishes CustomerRegistered v2 for every existing customer, so consumers whose queue did not exist when the
 * original event was published (the notification names, issue 232) catch up through the normal relay.
 */
class CustomerRegisteredBackfillIntegrationTest : CustomerIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }
    private val mapper = JsonMapper.builder().build()

    private data class OutboxRow(
        val eventId: UUID,
        val eventType: String,
        val eventVersion: Int,
        val aggregateId: String,
        val payload: JsonNode,
        val correlationId: String?,
        val published: Boolean,
    )

    private fun JdbcClient.outbox(): List<OutboxRow> =
        sql(
            "select event_id, event_type, event_version, aggregate_id, payload::text, correlation_id, " +
                "published_at is not null from outbox order by position",
        ).query { rs, _ ->
            OutboxRow(
                rs.getObject(1, UUID::class.java),
                rs.getString(2),
                rs.getInt(3),
                rs.getString(4),
                mapper.readTree(rs.getString(5)),
                rs.getString(6),
                rs.getBoolean(7),
            )
        }.list()

    private fun newDatabase(name: String): Pair<String, JdbcClient> {
        ownerJdbc.sql("create database $name").update()
        val url = postgres.jdbcUrl.replace(Regex("/[^/?]+(\\?|$)"), "/$name$1")
        return url to JdbcClient.create(DriverManagerDataSource(url, postgres.username, postgres.password))
    }

    private fun migrate(
        url: String,
        target: String?,
    ) = Flyway
        .configure()
        .dataSource(url, postgres.username, postgres.password)
        .placeholders(mapOf("appUser" to APP_USER))
        .apply { if (target != null) target(MigrationVersion.fromVersion(target)) }
        .load()
        .migrate()

    @Test
    fun `upgrading republishes one pending CustomerRegistered v2 per existing customer, equal to what registering writes`() {
        // What the application writes when a customer registers (the appender is the reference for envelope and payload).
        val reference = UUID.randomUUID()
        val referencePhone = nextPhone()
        api.post(
            "/customers",
            """{"name":"Ana Souza","email":"ana@example.test","phone":"$referencePhone"}""",
            JwtTestSupport.customerToken(reference),
        )
        val written = ownerJdbc.outbox().single { it.aggregateId == reference.toString() }

        val (url, jdbc) = newDatabase("backfill_upgrade")
        migrate(url, "4")
        val ids = List(3) { UUID.randomUUID() }
        ids.forEachIndexed { index, id ->
            val phone = if (index == 0) referencePhone else nextPhone()
            jdbc
                .sql("insert into customer (id, name, email, phone, status) values (:id, 'Ana Souza', 'ana@example.test', :phone, :status)")
                .param("id", id)
                .param("phone", phone)
                .param("status", if (index == 2) "BLOCKED" else "ACTIVE")
                .update()
            // The original event, already published to a topic nobody listened to.
            jdbc
                .sql(
                    "insert into outbox (event_id, aggregate_id, event_type, event_version, payload, occurred_at, published_at) " +
                        "values (gen_random_uuid(), :id, 'CustomerRegistered', 2, '{}'::jsonb, now(), now())",
                ).param("id", id.toString())
                .update()
        }
        val original = jdbc.outbox().map { it.eventId }.toSet()

        migrate(url, null)

        val pending = jdbc.outbox().filter { !it.published }
        assertThat(pending.map { it.aggregateId }).containsExactlyInAnyOrderElementsOf(ids.map(UUID::toString))
        assertThat(pending).allSatisfy {
            assertThat(it.eventType).isEqualTo("CustomerRegistered")
            assertThat(it.eventVersion).isEqualTo(2)
            assertThat(it.eventId).isNotIn(original)
            assertThat(it.correlationId).isNull()
            assertThat(it.payload.propertyNames().toList()).containsExactlyInAnyOrder("customerId", "name", "email", "phone")
            assertThat(it.payload.get("customerId").asString()).isEqualTo(it.aggregateId)
            assertThat(it.payload.get("name").asString()).isEqualTo("Ana Souza")
            assertThat(it.payload.get("email").asString()).isEqualTo("ana@example.test")
        }
        assertThat(pending.map { it.eventId }).doesNotHaveDuplicates()
        val sameAsAppender = pending.single { it.aggregateId == ids[0].toString() }
        assertThat(sameAsAppender.payload.get("phone").asString()).isEqualTo(referencePhone)
        assertThat(sameAsAppender.payload.propertyNames().toList()).isEqualTo(written.payload.propertyNames().toList())
        assertThat(sameAsAppender.eventVersion).isEqualTo(written.eventVersion)
        assertThat(sameAsAppender.eventType).isEqualTo(written.eventType)

        // Running Flyway again changes nothing.
        migrate(url, null)
        assertThat(jdbc.outbox()).hasSize(original.size + ids.size)
    }

    @Test
    fun `upgrading an empty database publishes nothing`() {
        val (url, jdbc) = newDatabase("backfill_empty")

        migrate(url, null)

        assertThat(jdbc.outbox()).isEmpty()
    }

    @Test
    fun `upgrading uses the current phone, not the one of the original event`() {
        val (url, jdbc) = newDatabase("backfill_phone")
        migrate(url, "4")
        val id = UUID.randomUUID()
        val current = nextPhone()
        jdbc
            .sql("insert into customer (id, name, email, phone, status) values (:id, 'Bia', 'bia@example.test', :phone, 'ACTIVE')")
            .param("id", id)
            .param("phone", current)
            .update()

        migrate(url, null)

        assertThat(
            jdbc
                .outbox()
                .single()
                .payload
                .get("phone")
                .asString(),
        ).isEqualTo(current)
    }
}
