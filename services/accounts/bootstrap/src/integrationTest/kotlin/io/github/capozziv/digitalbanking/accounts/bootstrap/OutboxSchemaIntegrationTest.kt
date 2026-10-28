package io.github.capozziv.digitalbanking.accounts.bootstrap

import io.github.capozziv.digitalbanking.outbox.assertOutboxSchemaMatchesModel
import org.junit.jupiter.api.Test
import org.springframework.jdbc.datasource.DriverManagerDataSource

/** The outbox table built by the accounts migrations (V4 + V6) must equal the model DDL of libs/outbox. */
class OutboxSchemaIntegrationTest : AccountsIntegrationTest() {
    @Test
    fun `outbox table migrated by accounts matches the model ddl`() {
        assertOutboxSchemaMatchesModel(DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password))
    }
}
