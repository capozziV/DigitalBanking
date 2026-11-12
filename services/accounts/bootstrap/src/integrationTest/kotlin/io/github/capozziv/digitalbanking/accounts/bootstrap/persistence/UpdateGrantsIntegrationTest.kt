package io.github.capozziv.digitalbanking.accounts.bootstrap.persistence

import io.github.capozziv.digitalbanking.accounts.bootstrap.AccountsIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.simple.JdbcClient
import java.sql.SQLException
import java.util.UUID

/** The application user may UPDATE only the columns the service writes in account_type and outbox. */
class UpdateGrantsIntegrationTest : AccountsIntegrationTest() {
    @Autowired private lateinit var appJdbc: JdbcClient

    @Test
    fun `the application user can update only overdraft_limit and updated_at of an account type`() {
        val updated =
            appJdbc
                .sql("update account_type set overdraft_limit = overdraft_limit, updated_at = now() where type = 'FREE'")
                .update()

        assertThat(updated).isEqualTo(1)
        assertThat(sqlStateOf { appJdbc.sql("update account_type set type = 'GONE' where type = 'FREE'").update() })
            .describedAs("update account_type set type")
            .isEqualTo(INSUFFICIENT_PRIVILEGE)
    }

    @Test
    fun `the application user can update only published_at, attempts and last_error of an outbox event`() {
        val eventId = UUID.randomUUID()
        try {
            appJdbc
                .sql(
                    """
                    insert into outbox (event_id, aggregate_id, event_type, event_version, payload, occurred_at)
                    values (:eventId, 'grants-test', 'GrantsTest', 1, '{}'::jsonb, now())
                    """.trimIndent(),
                ).param("eventId", eventId)
                .update()

            val updated =
                appJdbc
                    .sql("update outbox set published_at = now(), attempts = attempts + 1, last_error = 'boom' where event_id = :eventId")
                    .param("eventId", eventId)
                    .update()

            assertThat(updated).isEqualTo(1)
            val forbidden =
                listOf(
                    "position = position + 1000000",
                    "event_id = gen_random_uuid()",
                    "aggregate_id = 'other'",
                    "event_type = 'other'",
                    "event_version = 2",
                    "payload = '{\"x\":1}'::jsonb",
                    "correlation_id = 'other'",
                    "occurred_at = now()",
                )
            for (column in forbidden) {
                val sqlState =
                    sqlStateOf {
                        appJdbc
                            .sql("update outbox set $column where event_id = :eventId")
                            .param("eventId", eventId)
                            .update()
                    }
                assertThat(sqlState)
                    .describedAs("update outbox set %s", column)
                    .isEqualTo(INSUFFICIENT_PRIVILEGE)
            }
        } finally {
            markPendingOutboxEventsPublished()
        }
    }

    private fun sqlStateOf(statement: () -> Unit): String? {
        val error = runCatching(statement).exceptionOrNull()
        return generateSequence(error) { it.cause }.filterIsInstance<SQLException>().firstOrNull()?.sqlState
    }

    private companion object {
        const val INSUFFICIENT_PRIVILEGE = "42501"
    }
}
