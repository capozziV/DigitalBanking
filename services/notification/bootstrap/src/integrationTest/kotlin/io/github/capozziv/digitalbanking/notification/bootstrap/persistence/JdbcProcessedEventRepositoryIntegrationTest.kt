package io.github.capozziv.digitalbanking.notification.bootstrap.persistence

import io.github.capozziv.digitalbanking.notification.application.port.ProcessedEventRepository
import io.github.capozziv.digitalbanking.notification.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.notification.bootstrap.NotificationIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class JdbcProcessedEventRepositoryIntegrationTest : NotificationIntegrationTest() {
    @Autowired
    private lateinit var repository: ProcessedEventRepository

    @Autowired
    private lateinit var transactions: TransactionRunner

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

    @Test
    fun `two overlapping transactions on the same event let exactly one win`() {
        val eventId = UUID.randomUUID()
        val bothInserted = CountDownLatch(2)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val tasks =
                List(2) {
                    pool.submit<Boolean> {
                        transactions.inTransaction {
                            val first = repository.markProcessed(eventId, "AccountOpened", microsNow())
                            // The loser blocks on the winner's uncommitted row inside the insert, so the
                            // latch is only awaited with a timeout: the winner commits after it expires.
                            bothInserted.countDown()
                            bothInserted.await(500, TimeUnit.MILLISECONDS)
                            first
                        }
                    }
                }
            val results = tasks.map { it.get(30, TimeUnit.SECONDS) }

            assertThat(results).containsExactlyInAnyOrder(true, false)
            val count = ownerJdbc.sql("select count(*) from processed_events").query(Long::class.java).single()
            assertThat(count).isEqualTo(1L)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `rolled back mark lets a later transaction mark the same event`() {
        val eventId = UUID.randomUUID()

        runCatching {
            transactions.inTransaction {
                assertThat(repository.markProcessed(eventId, "AccountOpened", microsNow())).isTrue()
                error("boom")
            }
        }

        val second = transactions.inTransaction { repository.markProcessed(eventId, "AccountOpened", microsNow()) }
        assertThat(second).isTrue()
        val count = ownerJdbc.sql("select count(*) from processed_events").query(Long::class.java).single()
        assertThat(count).isEqualTo(1L)
    }
}
