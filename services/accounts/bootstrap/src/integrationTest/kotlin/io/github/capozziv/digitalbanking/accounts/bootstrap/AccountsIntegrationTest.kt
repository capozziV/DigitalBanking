package io.github.capozziv.digitalbanking.accounts.bootstrap

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Base of every integration test of the accounts service. One PostgreSQL container is shared by all
 * subclasses (singleton container, so the cached Spring context keeps a valid connection).
 * The application connects as [APP_USER] (no UPDATE/DELETE on ledger_entry); Flyway runs as the
 * container's owner user, mirroring accounts_owner / accounts_app of the local environment.
 * The outbox relay is off, so outbox rows stay as the operations wrote them; the relay tests turn it on
 * (a subclass @TestPropertySource overrides this one).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = ["accounts.outbox.relay-enabled=false"])
abstract class AccountsIntegrationTest {
    /** JdbcClient connected as the owner user, for setup and inspection outside the application's grants. */
    protected val ownerJdbc: JdbcClient by lazy {
        JdbcClient.create(DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password))
    }

    companion object {
        const val APP_USER = "accounts_app"
        const val APP_PASSWORD = "app"

        @JvmStatic
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer("postgres:17-alpine")
                .withInitScript("db/create-app-role.sql")
                .apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun databaseProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { APP_USER }
            registry.add("spring.datasource.password") { APP_PASSWORD }
            registry.add("spring.flyway.user") { postgres.username }
            registry.add("spring.flyway.password") { postgres.password }
            registry.add("spring.flyway.placeholders.appUser") { APP_USER }
        }
    }
}
