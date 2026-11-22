package io.github.capozziv.digitalbanking.transfers.bootstrap.saga

import org.awaitility.Awaitility
import org.awaitility.core.ConditionTimeoutException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.util.UUID
import javax.sql.DataSource

/** Polls the transfer row until it reaches [status]; fails with the last status seen after [timeout]. */
internal fun JdbcClient.awaitStatus(
    transferId: UUID,
    status: String,
    timeout: Duration = Duration.ofSeconds(20),
) {
    var current: String? = null
    try {
        Awaitility
            .await()
            .atMost(timeout)
            .pollInterval(Duration.ofMillis(50))
            .until {
                current = statusOf(transferId)
                current == status
            }
    } catch (e: ConditionTimeoutException) {
        error("transfer $transferId still $current after $timeout, expected $status")
    }
}

internal fun JdbcClient.statusOf(transferId: UUID): String? =
    sql("select status from transfer where id = :id")
        .param("id", transferId)
        .query(String::class.java)
        .optional()
        .orElse(null)

/** Steps recorded in `transfer_step` for the transfer, in order. */
internal fun JdbcClient.recordedSteps(transferId: UUID): List<String> =
    sql("select step from transfer_step where transfer_id = :id order by id")
        .param("id", transferId)
        .query { rs, _ -> rs.getString("step") }
        .list()

/** Correlation IDs of the outbox events of the transfer, by event type. */
internal fun JdbcClient.outboxCorrelationIds(transferId: UUID): Map<String, String?> =
    sql("select event_type, correlation_id from outbox where aggregate_id = :id order by position")
        .param("id", transferId.toString())
        .query { rs, _ -> rs.getString("event_type") to rs.getString("correlation_id") }
        .list()
        .toMap()

/**
 * Empties the saga tables in one transaction. The scheduler of an earlier test may still be recording a step, so the
 * transfers are locked first: `lock ... in exclusive mode` waits for the writers in flight and holds back the next ones
 * (the foreign key check of an insert into `transfer_step` or `transfer_manual_resolution` takes a row share lock on
 * `transfer`), and only then are the steps deleted. Deleting the steps in a transaction of their own left a window for a
 * step to commit before `delete from transfer`, which then violated the foreign key. Locking `transfer` first, and never
 * the child tables, keeps the lock order of the writers: a manual resolution inserts into `transfer_manual_resolution`
 * before it updates `transfer`, and that update goes ahead of the exclusive lock waiting in the queue, so there is no
 * cycle and it cannot deadlock.
 */
internal fun DataSource.deleteTransfers() {
    val jdbc = JdbcClient.create(this)
    TransactionTemplate(DataSourceTransactionManager(this)).executeWithoutResult {
        jdbc.sql("lock table transfer in exclusive mode").update()
        jdbc.sql("delete from transfer_step").update()
        jdbc.sql("delete from transfer_manual_resolution").update()
        jdbc.sql("delete from transfer").update()
        jdbc.sql("delete from outbox").update()
    }
}
