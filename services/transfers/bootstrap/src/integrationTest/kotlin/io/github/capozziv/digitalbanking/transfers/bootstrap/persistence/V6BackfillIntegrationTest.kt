package io.github.capozziv.digitalbanking.transfers.bootstrap.persistence

import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

/**
 * V6 fills `limit_day` of the transfers that existed before it with the São Paulo day of `created_at`. It runs on a
 * database of its own, migrated up to V5, so the rows exist before the column does.
 */
class V6BackfillIntegrationTest {
    private val postgres = TransfersIntegrationTest.postgres

    @Test
    fun `old transfers get the sao paulo day of their creation and stay unreserved`() {
        val url = freshDatabase()
        migrate(url, "5")
        val jdbc = JdbcClient.create(DriverManagerDataSource(url, postgres.username, postgres.password))
        val lastSecondOfThe12th = insertTransfer(jdbc, OffsetDateTime.parse("2026-11-13T02:59:59Z"))
        val firstSecondOfThe13th = insertTransfer(jdbc, OffsetDateTime.parse("2026-11-13T03:00:00Z"))

        migrate(url, "6")

        assertThat(limitOf(jdbc, lastSecondOfThe12th)).isEqualTo(LocalDate.parse("2026-11-12") to false)
        assertThat(limitOf(jdbc, firstSecondOfThe13th)).isEqualTo(LocalDate.parse("2026-11-13") to false)
    }

    private fun freshDatabase(): String {
        val admin = JdbcClient.create(DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password))
        admin.sql("drop database if exists $DATABASE").update()
        admin.sql("create database $DATABASE").update()
        return postgres.jdbcUrl.replace("/${postgres.databaseName}", "/$DATABASE")
    }

    private fun migrate(
        url: String,
        target: String,
    ) {
        Flyway
            .configure()
            .dataSource(url, postgres.username, postgres.password)
            .placeholders(mapOf("appUser" to TransfersIntegrationTest.APP_USER))
            .target(target)
            .load()
            .migrate()
    }

    private fun insertTransfer(
        jdbc: JdbcClient,
        createdAt: OffsetDateTime,
    ): UUID {
        val id = UUID.randomUUID()
        jdbc
            .sql(
                """
                insert into transfer (id, source_account_id, destination_account_id, amount, idempotency_key, status, step,
                                      deadline_at, next_attempt_at, attempts, created_at, updated_at)
                values (:id, :source, :destination, 100, :key, 'PENDING', 'DEBIT', :at, :at, 0, :at, :at)
                """.trimIndent(),
            ).param("id", id)
            .param("source", UUID.randomUUID())
            .param("destination", UUID.randomUUID())
            .param("key", "key-$id")
            .param("at", createdAt)
            .update()
        return id
    }

    private fun limitOf(
        jdbc: JdbcClient,
        id: UUID,
    ): Pair<LocalDate, Boolean> =
        jdbc
            .sql("select limit_day, limit_reserved from transfer where id = :id")
            .param("id", id)
            .query { rs, _ -> rs.getObject("limit_day", LocalDate::class.java) to rs.getBoolean("limit_reserved") }
            .single()

    private companion object {
        const val DATABASE = "v6_backfill"
    }
}
