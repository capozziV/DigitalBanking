package io.github.capozziv.digitalbanking.outbox

import org.assertj.core.api.Assertions.assertThat
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

private const val MODEL_DDL_RESOURCE = "/outbox/outbox-table.sql"

/**
 * Fails when the `outbox` table the service's migrations built differs from the model DDL of this library:
 * columns (name, type, nullability, default), indexes and constraints. The model is applied in a temporary schema
 * of the same database and dropped afterwards, so the connection's user needs permission to create schemas.
 */
fun assertOutboxSchemaMatchesModel(dataSource: DataSource) {
    val modelDdl =
        requireNotNull(OutboxSchemaSnapshot::class.java.getResource(MODEL_DDL_RESOURCE)) { "outbox model DDL not found" }
            .readText()
    dataSource.connection.use { connection ->
        val migratedSchema = connection.currentSchema()
        val modelSchema = "outbox_model_${UUID.randomUUID().toString().replace("-", "")}"
        val migrated = OutboxSchemaSnapshot.read(connection, migratedSchema)
        val model = createModelSchema(connection, modelSchema, modelDdl, migratedSchema)
        assertThat(migrated.columns).`as`("outbox columns").isEqualTo(model.columns)
        assertThat(migrated.indexes).`as`("outbox indexes").isEqualTo(model.indexes)
        assertThat(migrated.constraints).`as`("outbox constraints").isEqualTo(model.constraints)
    }
}

private fun createModelSchema(
    connection: Connection,
    modelSchema: String,
    modelDdl: String,
    migratedSchema: String,
): OutboxSchemaSnapshot {
    connection.createStatement().use { it.execute("create schema $modelSchema") }
    try {
        connection.createStatement().use {
            it.execute("set search_path to $modelSchema")
            it.execute(modelDdl)
        }
        return OutboxSchemaSnapshot.read(connection, modelSchema)
    } finally {
        connection.createStatement().use {
            it.execute("set search_path to $migratedSchema")
            it.execute("drop schema $modelSchema cascade")
        }
    }
}

private fun Connection.currentSchema(): String =
    createStatement().use { statement ->
        statement.executeQuery("select current_schema()").use { rs ->
            rs.next()
            rs.getString(1)
        }
    }

/** Description of the `outbox` table of one schema, free of the schema name so two schemas can be compared. */
private class OutboxSchemaSnapshot(
    val columns: List<String>,
    val indexes: List<String>,
    val constraints: List<String>,
) {
    companion object {
        fun read(
            connection: Connection,
            schema: String,
        ): OutboxSchemaSnapshot =
            OutboxSchemaSnapshot(
                columns =
                    query(
                        connection,
                        """
                        select column_name || ' ' || data_type || ' nullable=' || is_nullable ||
                               ' default=' || coalesce(column_default, 'none')
                        from information_schema.columns
                        where table_schema = ? and table_name = 'outbox'
                        order by column_name
                        """.trimIndent(),
                        schema,
                    ),
                indexes =
                    query(
                        connection,
                        "select indexdef from pg_indexes where schemaname = ? and tablename = 'outbox' order by indexname",
                        schema,
                    ),
                constraints =
                    query(
                        connection,
                        """
                        select conname || ' ' || pg_get_constraintdef(c.oid)
                        from pg_constraint c join pg_class t on t.oid = c.conrelid
                        join pg_namespace n on n.oid = t.relnamespace
                        where n.nspname = ? and t.relname = 'outbox'
                        order by conname
                        """.trimIndent(),
                        schema,
                    ),
            )

        private fun query(
            connection: Connection,
            sql: String,
            schema: String,
        ): List<String> =
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, schema)
                statement.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(rs.getString(1).replace("$schema.", ""))
                    }
                }
            }
    }
}
