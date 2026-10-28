package io.github.capozziv.digitalbanking.transfers.bootstrap

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Base of every integration test of the transfers service. One PostgreSQL container is shared by all subclasses
 * (singleton container, so the cached Spring context keeps a valid connection). The application connects as
 * [APP_USER]; Flyway runs as the container's owner user, mirroring transfers_owner / transfers_app of the
 * local environment. A shared WireMock server stands in for `accounts`; tests that use it reset it first.
 * The saga scheduler and the outbox relay are off, so transfers and outbox rows stay as the requests left them; the
 * tests that exercise them turn them on (a subclass @TestPropertySource overrides this one).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = ["transfers.saga.scheduler-enabled=false", "transfers.outbox.relay-enabled=false"])
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

        /** Stands in for the accounts service; `transfers.accounts.url` points to it. */
        @JvmStatic
        val accountsServer: WireMockServer = WireMockServer(options().dynamicPort()).apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun databaseProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { APP_USER }
            registry.add("spring.datasource.password") { APP_PASSWORD }
            registry.add("spring.flyway.user") { postgres.username }
            registry.add("spring.flyway.password") { postgres.password }
            registry.add("spring.flyway.placeholders.appUser") { APP_USER }
            registry.add("transfers.accounts.url") { "http://localhost:${accountsServer.port()}" }
        }
    }
}
