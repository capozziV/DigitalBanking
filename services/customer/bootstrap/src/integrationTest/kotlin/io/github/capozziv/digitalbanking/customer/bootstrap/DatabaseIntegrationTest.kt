package io.github.capozziv.digitalbanking.customer.bootstrap

import io.github.capozziv.digitalbanking.customer.domain.PhoneNumber
import io.github.capozziv.digitalbanking.outbox.assertOutboxSchemaMatchesModel
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.junit.jupiter.api.Test
import org.springframework.dao.DataAccessException
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.simple.JdbcClient
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
            .sql("insert into customer (id, name, email, phone, status) values (:id, 'Ana', 'ana@example.test', :phone, 'ACTIVE')")
            .param("id", id)
            .param("phone", nextPhone())
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
                .sql("insert into customer (id, name, email, phone, status) values (:id, 'Ana', 'ana@example.test', :phone, 'FROZEN')")
                .param("id", UUID.randomUUID())
                .param("phone", nextPhone())
                .update()
        }.isInstanceOf(DataAccessException::class.java)
    }

    @Test
    fun `the phone column only accepts a Brazilian mobile number in E164`() {
        listOf("11987654321", "+551134567890", "+5501987654321", "+55119876543210").forEach { phone ->
            assertThatThrownBy {
                appJdbc
                    .sql("insert into customer (id, name, email, phone, status) values (:id, 'Ana', 'ana@example.test', :phone, 'ACTIVE')")
                    .param("id", UUID.randomUUID())
                    .param("phone", phone)
                    .update()
            }.describedAs(phone).isInstanceOf(DataAccessException::class.java)
        }
    }

    @Test
    fun `two customers cannot have the same phone`() {
        val phone = nextPhone()
        val insert = "insert into customer (id, name, email, phone, status) values (:id, 'Ana', 'ana@example.test', :phone, 'ACTIVE')"
        appJdbc
            .sql(insert)
            .param("id", UUID.randomUUID())
            .param("phone", phone)
            .update()

        assertThatThrownBy {
            appJdbc
                .sql(insert)
                .param("id", UUID.randomUUID())
                .param("phone", phone)
                .update()
        }.isInstanceOf(DuplicateKeyException::class.java)
    }

    @Test
    fun `the migration gives each existing customer a distinct fictitious phone`() {
        ownerJdbc.sql("create database migration_check").update()
        val url = postgres.jdbcUrl.replace(Regex("/[^/?]+(\\?|$)"), "/migration_check$1")
        val migrate = { target: String ->
            Flyway
                .configure()
                .dataSource(url, postgres.username, postgres.password)
                .placeholders(mapOf("appUser" to APP_USER))
                .target(MigrationVersion.fromVersion(target))
                .load()
                .migrate()
        }
        migrate("3")
        val jdbc = JdbcClient.create(DriverManagerDataSource(url, postgres.username, postgres.password))
        repeat(3) {
            jdbc
                .sql("insert into customer (id, name, email, status) values (:id, 'Old', 'old@example.test', 'ACTIVE')")
                .param("id", UUID.randomUUID())
                .update()
        }

        migrate("4")

        val phones = jdbc.sql("select phone from customer").query(String::class.java).list()
        assertThat(phones).hasSize(3).doesNotHaveDuplicates().allMatch { PhoneNumber.PATTERN.toRegex().matches(it.orEmpty()) }
    }
}
