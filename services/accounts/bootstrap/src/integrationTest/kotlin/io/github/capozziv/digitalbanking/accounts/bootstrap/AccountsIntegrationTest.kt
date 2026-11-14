package io.github.capozziv.digitalbanking.accounts.bootstrap

import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.UUID

/**
 * Base of every integration test of the accounts service. One PostgreSQL container is shared by all
 * subclasses (singleton container, so the cached Spring context keeps a valid connection).
 * The application connects as [APP_USER] (no UPDATE/DELETE on ledger_entry); Flyway runs as the
 * container's owner user, mirroring accounts_owner / accounts_app of the local environment.
 * The outbox relay is off, so outbox rows stay as the operations wrote them; the relay tests turn it on
 * (a subclass @TestPropertySource overrides this one).
 *
 * Security: tokens are signed by [JwtTestSupport] (its decoder replaces the one built from the issuer URI), and the
 * token endpoint and the `customer` service are the [CustomerServiceStub], where every customer is ACTIVE by default.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = ["accounts.outbox.relay-enabled=false"])
@Import(JwtTestSupport.DecoderConfiguration::class)
abstract class AccountsIntegrationTest {
    /** JdbcClient connected as the owner user, for setup and inspection outside the application's grants. */
    protected val ownerJdbc: JdbcClient by lazy {
        JdbcClient.create(DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password))
    }

    protected val customerService: CustomerServiceStub get() = CustomerServiceStub.instance

    /** Marks as published every pending outbox event, such as those left by other tests on the shared database. */
    protected fun markPendingOutboxEventsPublished() {
        ownerJdbc.sql("update outbox set published_at = now() where published_at is null").update()
    }

    /** No account and no `AccountOpened` event exist for [customerId]. */
    protected fun assertNothingWrittenFor(customerId: UUID) {
        val accounts =
            ownerJdbc
                .sql("select count(*) from account where customer_id = :id")
                .param("id", customerId)
                .query(Long::class.java)
                .single()
        val events =
            ownerJdbc
                .sql("select count(*) from outbox where event_type = 'AccountOpened' and payload::text like :id")
                .param("id", "%$customerId%")
                .query(Long::class.java)
                .single()
        assertThat(accounts).isZero()
        assertThat(events).isZero()
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

        /** Set by a subclass (with its own @DynamicPropertySource) to point the `customer` service elsewhere. */
        const val CUSTOMER_BASE_URL_OVERRIDE = "accounts-test.customer-base-url"

        /** Set by a subclass (with its own @DynamicPropertySource) to point the token endpoint elsewhere. */
        const val TOKEN_URI_OVERRIDE = "accounts-test.token-uri"

        // The superclass's dynamic properties win over a subclass's for the same key, so the stub URLs are
        // placeholders whose default is the stub: a subclass overrides them through the *_OVERRIDE keys.
        @JvmStatic
        @DynamicPropertySource
        fun customerServiceProperties(registry: DynamicPropertyRegistry) {
            registry.add("accounts.customer.base-url") {
                "\${$CUSTOMER_BASE_URL_OVERRIDE:${CustomerServiceStub.instance.baseUrl}}"
            }
            registry.add("digitalbanking.security.service-token.token-uri") {
                "\${$TOKEN_URI_OVERRIDE:${CustomerServiceStub.instance.baseUrl}/token}"
            }
        }
    }
}
