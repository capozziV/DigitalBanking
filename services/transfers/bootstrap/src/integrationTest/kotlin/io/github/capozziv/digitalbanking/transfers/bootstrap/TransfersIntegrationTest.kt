package io.github.capozziv.digitalbanking.transfers.bootstrap

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Base of every integration test of the transfers service. One PostgreSQL container is shared by all subclasses
 * (singleton container, so the cached Spring context keeps a valid connection). The application connects as
 * [APP_USER]; Flyway runs as the container's owner user, mirroring transfers_owner / transfers_app of the
 * local environment.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class TransfersIntegrationTest {
    /** JdbcClient connected as the owner user, for setup and inspection outside the application's grants. */
    protected val ownerJdbc: JdbcClient by lazy {
        JdbcClient.create(DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password))
    }

    companion object {
        const val APP_USER = "transfers_app"
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
