package io.github.capozziv.digitalbanking.notification.bootstrap.persistence

import io.github.capozziv.digitalbanking.notification.application.port.ProcessedEventRepository
import io.github.capozziv.digitalbanking.notification.bootstrap.NotificationIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.OffsetDateTime
import java.util.UUID

class JdbcProcessedEventRepositoryIntegrationTest : NotificationIntegrationTest() {
    @Autowired
    private lateinit var repository: ProcessedEventRepository

    @BeforeEach
    fun cleanTables() {
        ownerJdbc.sql("delete from processed_events").update()
    }

    @Test
    fun `marks an event once and reports the repetition`() {
        val eventId = UUID.randomUUID()

        assertThat(repository.markProcessed(eventId, "AccountOpened", microsNow())).isTrue()
        assertThat(repository.markProcessed(eventId, "AccountOpened", microsNow())).isFalse()

        val count = ownerJdbc.sql("select count(*) from processed_events").query(Long::class.java).single()
        assertThat(count).isEqualTo(1L)
    }

    @Test
    fun `stores the event type and the processing instant`() {
        val eventId = UUID.randomUUID()
        val at = microsNow()

        repository.markProcessed(eventId, "TransferCompleted", at)

        val row =
            ownerJdbc
                .sql("select event_type, processed_at from processed_events where event_id = :id")
                .param("id", eventId)
                .query { rs, _ -> rs.getString("event_type") to rs.getObject("processed_at", OffsetDateTime::class.java) }
                .single()
        assertThat(row.first).isEqualTo("TransferCompleted")
        assertThat(row.second.toInstant()).isEqualTo(at)
    }
}
