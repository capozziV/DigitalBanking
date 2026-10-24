package io.github.capozziv.digitalbanking.outbox

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Testcontainers
class JdbcOutboxWriterIntegrationTest {
    private val now = Instant.parse("2026-10-22T14:31:00Z")
    private lateinit var jdbcClient: JdbcClient
    private lateinit var writer: JdbcOutboxWriter

    @BeforeEach
    fun setUp() {
        val dataSource = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
        jdbcClient = JdbcClient.create(dataSource)
        jdbcClient.sql("drop table if exists outbox").update()
        jdbcClient.sql(outboxDdl()).update()
        writer = JdbcOutboxWriter(jdbcClient, Clock.fixed(now, ZoneOffset.UTC))
    }

    @AfterEach
    fun clearMdc() {
        MDC.clear()
    }

    @Test
    fun `write stores an unpublished row with the message, the clock instant and the MDC correlation id`() {
        MDC.put(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY, "corr-123")
        val payload = """{ "accountId": "acc-1",  "amount": 3700, "nested": {"kind": "CREDIT"} }"""

        writer.write(OutboxMessage("LedgerEntryRecorded", 1, "acc-1", payload))

        val row = jdbcClient.sql("select * from outbox").query().singleRow()
        assertThat(row["event_type"]).isEqualTo("LedgerEntryRecorded")
        assertThat(row["event_version"]).isEqualTo(1)
        assertThat(row["aggregate_id"]).isEqualTo("acc-1")
        assertThat(row["correlation_id"]).isEqualTo("corr-123")
        assertThat(row["published_at"]).isNull()
        assertThat(row["attempts"]).isEqualTo(0)
        assertThat(row["last_error"]).isNull()
        assertThat(
            jdbcClient
                .sql("select occurred_at from outbox")
                .query(OffsetDateTime::class.java)
                .single()
                .toInstant(),
        ).isEqualTo(now)
        assertThat(
            jdbcClient
                .sql("select payload = cast(:expected as jsonb) from outbox")
                .param("expected", payload)
                .query(Boolean::class.java)
                .single(),
        ).isTrue()
    }

    @Test
    fun `write stores a null correlation id when the MDC has none`() {
        writer.write(OutboxMessage("AccountOpened", 1, "acc-1", """{"accountId":"acc-1"}"""))

        assertThat(
            jdbcClient.sql("select correlation_id from outbox").query().singleRow()["correlation_id"],
        ).isNull()
    }

    @Test
    fun `consecutive writes get increasing positions and distinct version 7 event ids`() {
        writer.write(OutboxMessage("AccountOpened", 1, "acc-1", """{"accountId":"acc-1"}"""))
        writer.write(OutboxMessage("AccountOpened", 1, "acc-2", """{"accountId":"acc-2"}"""))

        val rows =
            jdbcClient
                .sql("select position, event_id, aggregate_id from outbox order by position")
                .query()
                .listOfRows()
        assertThat(rows.map { it["aggregate_id"] }).containsExactly("acc-1", "acc-2")
        assertThat(rows[1]["position"] as Long).isGreaterThan(rows[0]["position"] as Long)
        val eventIds = rows.map { it["event_id"] as UUID }
        assertThat(eventIds).doesNotHaveDuplicates()
        assertThat(eventIds.map { it.version() }).containsOnly(7)
    }

    private fun outboxDdl(): String =
        requireNotNull(javaClass.getResource("/outbox/outbox-table.sql")) { "outbox DDL not found" }.readText()

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17-alpine")
    }
}
