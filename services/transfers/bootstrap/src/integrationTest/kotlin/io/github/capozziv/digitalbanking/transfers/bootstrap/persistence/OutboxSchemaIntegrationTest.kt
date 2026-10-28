package io.github.capozziv.digitalbanking.transfers.bootstrap.persistence

import io.github.capozziv.digitalbanking.outbox.assertOutboxSchemaMatchesModel
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.jdbc.datasource.DriverManagerDataSource

/** The outbox table built by the transfers migrations (V3) must equal the model DDL of libs/outbox. */
class OutboxSchemaIntegrationTest : TransfersIntegrationTest() {
    @Test
    fun `outbox table migrated by transfers matches the model ddl`() {
        assertOutboxSchemaMatchesModel(DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password))
    }
}
