package io.github.capozziv.digitalbanking.notification.adapters.persistence

import io.github.capozziv.digitalbanking.notification.application.port.NotificationCursor
import io.github.capozziv.digitalbanking.notification.application.port.NotificationRepository
import io.github.capozziv.digitalbanking.notification.domain.Notification
import io.github.capozziv.digitalbanking.notification.domain.NotificationKind
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Plain SQL over [JdbcClient]. The page order is `(created_at desc, id desc)`; the cursor and
 * [markReadUpTo] compare the same `(created_at, id)` tuple, so they agree on what "up to" means.
 */
@Repository
class JdbcNotificationRepository(
    private val jdbcClient: JdbcClient,
) : NotificationRepository {
    override fun saveAll(notifications: List<Notification>) {
        notifications.forEach { notification ->
            jdbcClient
                .sql(
                    """
                    insert into notifications (id, customer_id, kind, title, body, data, source_event_id, created_at, read_at)
                    values (:id, :customerId, :kind, :title, :body, cast(:data as jsonb), :sourceEventId, :createdAt, :readAt)
                    """.trimIndent(),
                ).param("id", notification.id)
                .param("customerId", notification.customerId)
                .param("kind", notification.kind.name)
                .param("title", notification.title)
                .param("body", notification.body)
                .param("data", JSON.writeValueAsString(notification.data))
                .param("sourceEventId", notification.sourceEventId)
                .param("createdAt", notification.createdAt.utc())
                .param("readAt", notification.readAt?.utc())
                .update()
        }
    }

    override fun page(
        customerId: UUID,
        cursor: NotificationCursor?,
        size: Int,
    ): List<Notification> {
        val after = if (cursor == null) "" else "and (created_at, id) < (:cursorCreatedAt, :cursorId)"
        val statement =
            jdbcClient
                .sql(
                    """
                    select $COLUMNS from notifications
                    where customer_id = :customerId $after
                    order by created_at desc, id desc
                    limit :size
                    """.trimIndent(),
                ).param("customerId", customerId)
                .param("size", size)
        if (cursor != null) {
            statement.param("cursorCreatedAt", cursor.createdAt.utc()).param("cursorId", cursor.id)
        }
        return statement.query(ROW_MAPPER).list()
    }

    override fun countUnread(customerId: UUID): Long =
        jdbcClient
            .sql("select count(*) from notifications where customer_id = :customerId and read_at is null")
            .param("customerId", customerId)
            .query(Long::class.java)
            .single()

    override fun find(id: UUID): Notification? =
        jdbcClient
            .sql("select $COLUMNS from notifications where id = :id")
            .param("id", id)
            .query(ROW_MAPPER)
            .optional()
            .orElse(null)

    override fun markReadUpTo(
        customerId: UUID,
        upTo: Notification,
        at: Instant,
    ): Int =
        jdbcClient
            .sql(
                """
                update notifications set read_at = :at
                where customer_id = :customerId and read_at is null
                  and (created_at, id) <= (:upToCreatedAt, :upToId)
                """.trimIndent(),
            ).param("at", at.utc())
            .param("customerId", customerId)
            .param("upToCreatedAt", upTo.createdAt.utc())
            .param("upToId", upTo.id)
            .update()

    private fun Instant.utc(): OffsetDateTime = OffsetDateTime.ofInstant(this, ZoneOffset.UTC)

    private companion object {
        const val COLUMNS = "id, customer_id, kind, title, body, data::text as data, source_event_id, created_at, read_at"

        val JSON: JsonMapper = JsonMapper.builder().build()
        val DATA_TYPE = object : TypeReference<Map<String, String>>() {}

        val ROW_MAPPER =
            RowMapper { rs, _ ->
                Notification(
                    id = rs.getObject("id", UUID::class.java),
                    customerId = rs.getObject("customer_id", UUID::class.java),
                    kind = NotificationKind.valueOf(rs.getString("kind")),
                    title = rs.getString("title"),
                    body = rs.getString("body"),
                    data = JSON.readValue(rs.getString("data"), DATA_TYPE),
                    sourceEventId = rs.getObject("source_event_id", UUID::class.java),
                    createdAt = rs.getObject("created_at", OffsetDateTime::class.java).toInstant(),
                    readAt = rs.getObject("read_at", OffsetDateTime::class.java)?.toInstant(),
                )
            }
    }
}
