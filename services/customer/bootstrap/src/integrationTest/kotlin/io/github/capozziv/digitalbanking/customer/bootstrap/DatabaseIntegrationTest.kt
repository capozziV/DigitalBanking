package io.github.capozziv.digitalbanking.customer.bootstrap

import io.github.capozziv.digitalbanking.outbox.assertOutboxSchemaMatchesModel
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.util.UUID

/** The schema of the migrations and the grants of the application user. */
class DatabaseIntegrationTest : CustomerIntegrationTest() {
    @Test
    fun `outbox table migrated by customer matches the model ddl`() {
        assertOutboxSchemaMatchesModel(DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password))
    }

    @Test
    fun `the application user reads, inserts and updates customer and outbox but never deletes`() {
        val id = UUID.randomUUID()
        appJdbc
            .sql("insert into customer (id, name, email, status) values (:id, 'Ana', 'ana@example.test', 'ACTIVE')")
            .param("id", id)
            .update()
        assertThat(appJdbc.sql("update customer set status = 'BLOCKED' where id = :id").param("id", id).update()).isEqualTo(1)
        assertThat(
            appJdbc
                .sql("select count(*) from customer where id = :id")
                .param("id", id)
                .query(Long::class.java)
                .single(),
        ).isEqualTo(1L)
        assertThat(appJdbc.sql("select count(*) from outbox").query(Long::class.java).single()).isNotNull()

        assertThatThrownBy { appJdbc.sql("delete from customer where id = :id").param("id", id).update() }
            .isInstanceOf(DataAccessException::class.java)
        assertThatThrownBy { appJdbc.sql("delete from outbox").update() }.isInstanceOf(DataAccessException::class.java)
        assertThatThrownBy { appJdbc.sql("create table intruder (id int)").update() }.isInstanceOf(DataAccessException::class.java)
    }

    @Test
    fun `the status column only accepts ACTIVE and BLOCKED`() {
        assertThatThrownBy {
            appJdbc
                .sql("insert into customer (id, name, email, status) values (:id, 'Ana', 'ana@example.test', 'FROZEN')")
                .param("id", UUID.randomUUID())
                .update()
        }.isInstanceOf(DataAccessException::class.java)
    }
}
