package io.github.capozziv.digitalbanking.notification.bootstrap

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Base of the integration tests of the notification service. One PostgreSQL container is shared by all subclasses.
 * The application connects as [APP_USER]; Flyway runs as the container's owner user, mirroring
 * notification_owner / notification_app of the local environment.
 *
 * The properties are in [TestPropertySource], which subclasses inherit and merge, so a subclass's own
 * `@SpringBootTest` (e.g. a web environment) does not drop them.
 */
@SpringBootTest
@TestPropertySource(
    properties = [
        "spring.cloud.aws.region.static=us-east-1",
        // No SQS here, and never the real AWS: an explicit endpoint where nothing listens.
        "spring.cloud.aws.sqs.endpoint=http://127.0.0.1:1",
        // The queue listeners stay stopped (NotificationConsumptionIntegrationTest covers them).
        "spring.cloud.aws.sqs.listener.auto-startup=false",
        // The DLQ gauge reads SQS once at startup (and fails, reporting NaN); no further reads during the tests.
        "notification.dlq-metrics.refresh-interval=1h",
    ],
)
abstract class NotificationIntegrationTest {
    /** JdbcClient connected as the owner user, for setup and inspection outside the application's grants. */
    protected val ownerJdbc: JdbcClient by lazy {
        JdbcClient.create(DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password))
    }

    companion object {
        const val APP_USER = "notification_app"
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
