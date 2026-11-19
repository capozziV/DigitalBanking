package io.github.capozziv.digitalbanking.testing.outboxcontract

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.postgresql.PostgreSQLContainer
import javax.sql.DataSource

/**
 * PostgreSQL (in a container, one per JVM) with the model outbox table of libs/outbox, which every service's migration
 * is checked against (assertOutboxSchemaMatchesModel). The container starts on first use.
 */
object OutboxContractDatabase {
    private val postgres: PostgreSQLContainer by lazy {
        PostgreSQLContainer(IMAGE).apply { start() }
    }

    val dataSource: DataSource by lazy {
        DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password).also { dataSource ->
            JdbcClient.create(dataSource).sql(outboxDdl()).update()
        }
    }

    private fun outboxDdl(): String =
        requireNotNull(OutboxContractDatabase::class.java.getResource(OUTBOX_DDL)) { "outbox DDL $OUTBOX_DDL not found" }
            .readText()

    private const val IMAGE = "postgres:17-alpine"
    private const val OUTBOX_DDL = "/outbox/outbox-table.sql"
}
