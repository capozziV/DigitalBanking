package io.github.capozziv.digitalbanking.outbox

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer

@Testcontainers
class OutboxSchemaAssertionsIntegrationTest {
    private val dataSource = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
    private val jdbcClient = JdbcClient.create(dataSource)

    @BeforeEach
    fun createModelTable() {
        jdbcClient.sql("drop table if exists outbox").update()
        jdbcClient.sql(modelDdl()).update()
    }

    @Test
    fun `table created from the model ddl matches the model`() {
        assertThatCode { assertOutboxSchemaMatchesModel(dataSource) }.doesNotThrowAnyException()
    }

    @Test
    fun `an extra column makes the assertion fail`() {
        jdbcClient.sql("alter table outbox add column tenant text").update()

        assertThatThrownBy { assertOutboxSchemaMatchesModel(dataSource) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("tenant")
    }

    @Test
    fun `a missing index makes the assertion fail`() {
        jdbcClient.sql("drop index outbox_stuck_idx").update()

        assertThatThrownBy { assertOutboxSchemaMatchesModel(dataSource) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("outbox_stuck_idx")
    }

    @Test
    fun `a changed column nullability makes the assertion fail`() {
        jdbcClient.sql("alter table outbox alter column event_type drop not null").update()

        assertThatThrownBy { assertOutboxSchemaMatchesModel(dataSource) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("event_type")
    }

    @Test
    fun `a changed column default makes the assertion fail`() {
        jdbcClient.sql("alter table outbox alter column attempts set default 5").update()

        assertThatThrownBy { assertOutboxSchemaMatchesModel(dataSource) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("attempts")
    }

    @Test
    fun `a missing constraint makes the assertion fail`() {
        jdbcClient.sql("alter table outbox drop constraint outbox_event_id_key").update()

        assertThatThrownBy { assertOutboxSchemaMatchesModel(dataSource) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("outbox_event_id_key")
    }

    private fun modelDdl(): String = requireNotNull(javaClass.getResource("/outbox/outbox-table.sql")) { "outbox DDL not found" }.readText()

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17-alpine")
    }
}
