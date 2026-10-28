package io.github.capozziv.digitalbanking.transfers.bootstrap.saga

import org.springframework.jdbc.core.simple.JdbcClient
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** Polls the transfer row until it reaches [status]; fails with the last status seen after [timeout]. */
internal fun JdbcClient.awaitStatus(
    transferId: UUID,
    status: String,
    timeout: Duration = Duration.ofSeconds(20),
) {
    val deadline = Instant.now().plus(timeout)
    var current: String? = null
    while (Instant.now().isBefore(deadline)) {
        current = statusOf(transferId)
        if (current == status) return
        Thread.sleep(50)
    }
    error("transfer $transferId still $current after $timeout, expected $status")
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

internal fun JdbcClient.deleteTransfers() {
    sql("delete from transfer_step").update()
    sql("delete from transfer").update()
    sql("delete from outbox").update()
}
