package io.github.capozziv.digitalbanking.notification.adapters.persistence

import io.github.capozziv.digitalbanking.notification.application.port.ProcessedEventRepository
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
class JdbcProcessedEventRepository(
    private val jdbcClient: JdbcClient,
) : ProcessedEventRepository {
    override fun markProcessed(
        eventId: UUID,
        eventType: String,
        at: Instant,
    ): Boolean =
        jdbcClient
            .sql(
                """
                insert into processed_events (event_id, event_type, processed_at)
                values (:eventId, :eventType, :at)
                on conflict (event_id) do nothing
                """.trimIndent(),
            ).param("eventId", eventId)
            .param("eventType", eventType)
            .param("at", OffsetDateTime.ofInstant(at, ZoneOffset.UTC))
            .update() == 1
}
