package io.github.capozziv.digitalbanking.outbox

import com.fasterxml.uuid.Generators
import org.slf4j.MDC
import org.springframework.jdbc.core.simple.JdbcClient
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Records outbox messages through the given [JdbcClient], so the insert joins the caller's
 * connection and transaction. Publishing is the relay's job.
 */
class JdbcOutboxWriter(
    private val jdbcClient: JdbcClient,
    private val clock: Clock,
) {
    private val eventIdGenerator = Generators.timeBasedEpochGenerator()

    fun write(message: OutboxMessage) {
        jdbcClient
            .sql(
                """
                insert into outbox (event_id, aggregate_id, event_type, event_version, payload, correlation_id, occurred_at)
                values (:eventId, :aggregateId, :eventType, :eventVersion, cast(:payload as jsonb), :correlationId, :occurredAt)
                """.trimIndent(),
            ).param("eventId", eventIdGenerator.generate())
            .param("aggregateId", message.aggregateId)
            .param("eventType", message.eventType)
            .param("eventVersion", message.eventVersion)
            .param("payload", message.payloadJson)
            .param("correlationId", MDC.get(OutboxEnvelope.CORRELATION_ID_MDC_KEY))
            .param("occurredAt", OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC))
            .update()
    }
}
