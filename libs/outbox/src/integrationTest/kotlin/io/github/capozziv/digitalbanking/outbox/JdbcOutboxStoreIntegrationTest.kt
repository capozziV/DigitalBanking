package io.github.capozziv.digitalbanking.outbox

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Testcontainers
class JdbcOutboxStoreIntegrationTest {
    private val now = Instant.parse("2026-10-22T14:31:00.123456Z")
    private lateinit var jdbcClient: JdbcClient
    private lateinit var transactions: TransactionTemplate
    private lateinit var writer: JdbcOutboxWriter
    private lateinit var store: JdbcOutboxStore

    @BeforeEach
    fun setUp() {
        val dataSource = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
        jdbcClient = JdbcClient.create(dataSource)
        jdbcClient.sql("drop table if exists outbox").update()
        jdbcClient.sql(outboxDdl()).update()
        transactions = TransactionTemplate(DataSourceTransactionManager(dataSource))
        writer = JdbcOutboxWriter(jdbcClient, Clock.fixed(now, ZoneOffset.UTC))
        store = JdbcOutboxStore(jdbcClient, transactions)
    }

    @Test
    fun `only one of two concurrent asLeader calls runs its block`() {
        val executed = AtomicInteger()
        val leaderInside = CountDownLatch(1)
        val releaseLeader = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first =
                executor.submit<String?> {
                    store.asLeader(LOCK_KEY) {
                        executed.incrementAndGet()
                        leaderInside.countDown()
                        releaseLeader.await(10, TimeUnit.SECONDS)
                        "first"
                    }
                }
            assertThat(leaderInside.await(10, TimeUnit.SECONDS)).isTrue()

            val second =
                executor.submit<String?> {
                    store.asLeader(LOCK_KEY) {
                        executed.incrementAndGet()
                        "second"
                    }
                }
            assertThat(second.get(10, TimeUnit.SECONDS)).isNull()

            releaseLeader.countDown()
            assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo("first")
            assertThat(executed.get()).isEqualTo(1)
        } finally {
            releaseLeader.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `the lock is released when the leader transaction ends`() {
        assertThat(store.asLeader(LOCK_KEY) { "first" }).isEqualTo("first")
        assertThat(store.asLeader(LOCK_KEY) { "second" }).isEqualTo("second")
    }

    @Test
    fun `a different lock key does not compete for leadership`() {
        val result = store.asLeader(LOCK_KEY) { store.asLeaderOnOtherConnection(LOCK_KEY + 1) ?: "not leader" }

        assertThat(result).isEqualTo("other")
    }

    @Test
    fun `asLeader rolls back the block work when it throws`() {
        write("acc-1")
        val position = positions().single()

        runCatching { store.asLeader(LOCK_KEY) { store.markPublished(position, now).also { error("boom") } } }

        assertThat(store.asLeader(LOCK_KEY) { store.fetchPending(10) }!!.map { it.position }).containsExactly(position)
    }

    @Test
    fun `fetchPending returns unpublished events in position order, up to the limit`() {
        write("acc-1")
        write("acc-2")
        write("acc-1")
        write("acc-3")
        val (p1, p2, p3, p4) = positions()
        store.asLeader(LOCK_KEY) { store.markPublished(p2, now) }

        val pending = store.asLeader(LOCK_KEY) { store.fetchPending(2) }!!

        assertThat(pending.map { it.position }).containsExactly(p1, p3)
        assertThat(store.asLeader(LOCK_KEY) { store.fetchPending(10) }!!.map { it.position }).containsExactly(p1, p3, p4)
    }

    @Test
    fun `fetchPending offers only the head event of an account with a failed event, so other accounts still fit the batch`() {
        repeat(3) { write("acc-A") }
        write("acc-B")
        val (a1, _, _, b1) = positions()
        assertThat(store.asLeader(LOCK_KEY) { store.fetchPending(3) }!!.map { it.aggregateId }).containsOnly("acc-A")

        store.asLeader(LOCK_KEY) { store.markFailed(a1, "sns down") }

        assertThat(store.asLeader(LOCK_KEY) { store.fetchPending(3) }!!.map { it.position }).containsExactly(a1, b1)
    }

    @Test
    fun `fetchPending releases the rest of an account once its failed head event is published`() {
        repeat(3) { write("acc-A") }
        val (a1, a2, a3) = positions()
        store.asLeader(LOCK_KEY) { store.markFailed(a1, "sns down") }

        store.asLeader(LOCK_KEY) { store.markPublished(a1, now) }

        assertThat(store.asLeader(LOCK_KEY) { store.fetchPending(10) }!!.map { it.position }).containsExactly(a2, a3)
    }

    @Test
    fun `fetchPending maps every column of the row`() {
        MDC.put(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY, "corr-9")
        try {
            write("acc-1", payload = """{"amount": 3700}""")
        } finally {
            MDC.clear()
        }
        val row = jdbcClient.sql("select position, event_id from outbox").query().singleRow()

        val event = store.asLeader(LOCK_KEY) { store.fetchPending(10) }!!.single()

        assertThat(event.position).isEqualTo(row["position"])
        assertThat(event.eventId).isEqualTo(row["event_id"])
        assertThat(event.aggregateId).isEqualTo("acc-1")
        assertThat(event.eventType).isEqualTo("LedgerEntryRecorded")
        assertThat(event.eventVersion).isEqualTo(1)
        assertThat(event.payloadJson).isEqualTo("""{"amount": 3700}""")
        assertThat(event.correlationId).isEqualTo("corr-9")
        assertThat(event.occurredAt).isEqualTo(now)
        assertThat(event.attempts).isZero()
    }

    @Test
    fun `fetchPending is served by the partial index on unpublished rows`() {
        val plan =
            transactions.execute {
                jdbcClient.sql("set local enable_seqscan = off").update()
                jdbcClient
                    .sql("explain ${JdbcOutboxStore.FETCH_PENDING_SQL}")
                    .param("limit", 10)
                    .query(String::class.java)
                    .list()
                    .joinToString("\n")
            }

        assertThat(plan).contains("outbox_unpublished_idx")
    }

    @Test
    fun `fetchPending reads a large, mostly published outbox without scanning the table`() {
        jdbcClient
            .sql(
                """
                insert into outbox (event_id, aggregate_id, event_type, event_version, payload, occurred_at, published_at)
                select gen_random_uuid(), 'acc-' || (g % 500), 'T', 1, '{}', now(), case when g <= 19000 then now() end
                from generate_series(1, 20000) g
                """.trimIndent(),
            ).update()
        jdbcClient.sql("update outbox set attempts = 1 where position in (19001, 19050)").update()
        jdbcClient.sql("analyze outbox").update()

        val plan =
            jdbcClient
                .sql("explain ${JdbcOutboxStore.FETCH_PENDING_SQL}")
                .param("limit", 100)
                .query(String::class.java)
                .list()
                .joinToString("\n")

        assertThat(plan).contains("outbox_unpublished_idx", "outbox_stuck_idx").doesNotContain("Seq Scan")
    }

    @Test
    fun `markPublished stores the instant and markFailed increments attempts and keeps the last error`() {
        write("acc-1")
        write("acc-2")
        val (published, failed) = positions()

        store.asLeader(LOCK_KEY) {
            store.markPublished(published, now)
            store.markFailed(failed, "first error")
            store.markFailed(failed, "second error")
        }

        assertThat(
            jdbcClient
                .sql("select published_at from outbox where position = :p")
                .param("p", published)
                .query(OffsetDateTime::class.java)
                .single()
                .toInstant(),
        ).isEqualTo(now)
        val failedRow =
            jdbcClient
                .sql("select * from outbox where position = :p")
                .param("p", failed)
                .query()
                .singleRow()
        assertThat(failedRow["attempts"]).isEqualTo(2)
        assertThat(failedRow["last_error"]).isEqualTo("second error")
        assertThat(failedRow["published_at"]).isNull()
        assertThat(store.asLeader(LOCK_KEY) { store.fetchPending(10) }!!.single().attempts).isEqualTo(2)
    }

    @Test
    fun `backlog counts unpublished events and measures the age of the oldest`() {
        write("acc-1")
        write("acc-2")
        val (first, _) = positions()
        store.asLeader(LOCK_KEY) { store.markPublished(first, now) }
        jdbcClient
            .sql("update outbox set occurred_at = :at where published_at is null")
            .param("at", OffsetDateTime.ofInstant(now.minusSeconds(30), ZoneOffset.UTC))
            .update()

        val backlog = store.backlog(now)

        assertThat(backlog).isEqualTo(OutboxBacklog(pending = 1, oldestAge = Duration.ofSeconds(30)))
    }

    @Test
    fun `backlog of an empty outbox has no age`() {
        assertThat(store.backlog(now)).isEqualTo(OutboxBacklog(pending = 0, oldestAge = null))
    }

    private fun JdbcOutboxStore.asLeaderOnOtherConnection(lockKey: Long): String? {
        val executor = Executors.newSingleThreadExecutor()
        try {
            return executor.submit<String?> { asLeader(lockKey) { "other" } }.get(10, TimeUnit.SECONDS)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun write(
        aggregateId: String,
        payload: String = """{"accountId":"$aggregateId"}""",
    ) = writer.write(OutboxMessage("LedgerEntryRecorded", 1, aggregateId, payload))

    private fun positions(): List<Long> =
        jdbcClient
            .sql("select position from outbox order by position")
            .query { rs, _ -> rs.getLong("position") }
            .list()

    private fun outboxDdl(): String =
        requireNotNull(javaClass.getResource("/outbox/outbox-table.sql")) { "outbox DDL not found" }.readText()

    companion object {
        private const val LOCK_KEY = 4_242L

        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17-alpine")
    }
}
