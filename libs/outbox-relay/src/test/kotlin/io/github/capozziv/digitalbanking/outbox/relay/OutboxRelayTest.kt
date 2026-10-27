package io.github.capozziv.digitalbanking.outbox.relay

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.github.capozziv.digitalbanking.outbox.OutboxEventEnvelope
import io.github.capozziv.digitalbanking.outbox.PendingEvent
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

class OutboxRelayTest {
    private val now = Instant.parse("2026-10-22T14:31:00Z")
    private val clock = MutableClock(now)
    private val store = FakeOutboxStore()
    private val publisher = FakeEventPublisher()
    private val registry = SimpleMeterRegistry()
    private val settings =
        OutboxRelaySettings(batchSize = 50, alertAfterAttempts = 10, lockKey = 42, maxCycleDuration = Duration.ofSeconds(10))
    private val relay = relay(settings)

    private val logger = LoggerFactory.getLogger(OutboxRelay::class.java) as Logger
    private val logs = ListAppender<ILoggingEvent>()

    @BeforeEach
    fun attachAppender() {
        logs.start()
        logger.addAppender(logs)
    }

    @AfterEach
    fun detachAppender() {
        logger.detachAppender(logs)
        MDC.clear()
    }

    @Test
    fun `publishes pending events in position order and marks each one published at the clock instant`() {
        store.add(event(1, "A"), event(2, "B"), event(3, "A"))

        relay.runCycle()

        assertThat(publisher.published.map { it.position }).containsExactly(1, 2, 3)
        assertThat(store.publishedAt).containsExactlyEntriesOf(mapOf(1L to now, 2L to now, 3L to now))
    }

    @Test
    fun `fetches at most the configured batch size under the lock key, inside the leadership`() {
        relay.runCycle()

        assertThat(store.leaderLockKeys).containsExactly(42L)
        assertThat(store.fetchLimits).containsExactly(50)
        assertThat(store.callsOutsideLeadership).isEmpty()
    }

    @Test
    fun `failure on an event blocks the rest of its account in the cycle but not other accounts`() {
        store.add(event(1, "A"), event(2, "A"), event(3, "B"), event(4, "A"), event(5, "B"))
        publisher.failOn(2)

        relay.runCycle()

        assertThat(publisher.published.map { it.position }).containsExactly(1, 3, 5)
        assertThat(publisher.attempted.map { it.position }).containsExactly(1, 2, 3, 5)
        assertThat(store.publishedAt.keys).containsExactly(1L, 3L, 5L)
        assertThat(store.failures.keys).containsExactly(2L)
    }

    @Test
    fun `a stuck account filling a whole batch does not hold back other accounts in the next cycle`() {
        val relay = relay(settings.copy(batchSize = 3))
        store.add(event(1, "A"), event(2, "A"), event(3, "A"), event(4, "B"))
        publisher.failOn(1)

        relay.runCycle()
        assertThat(publisher.attempted.map { it.position }).containsExactly(1)

        relay.runCycle()

        assertThat(publisher.attempted.map { it.position }).containsExactly(1, 1, 4)
        assertThat(store.publishedAt.keys).containsExactly(4L)
    }

    @Test
    fun `failure is recorded with the error message`() {
        store.add(event(1, "A"))
        publisher.failOn(1)

        relay.runCycle()

        assertThat(store.failures.getValue(1)).contains("sns down for 1")
    }

    @Test
    fun `failure below the alert threshold logs a warning with the error message and no stack trace`() {
        store.add(event(1, "A", attempts = 8))
        publisher.failOn(1)

        relay.runCycle()

        val warning = logs.list.single { it.level == Level.WARN }
        assertThat(warning.formattedMessage).contains("sns down for 1")
        assertThat(warning.throwableProxy).isNull()
        assertThat(logs.list.map { it.level }).doesNotContain(Level.ERROR)
    }

    @Test
    fun `failure that reaches the alert threshold logs an error with the event, its correlation id and the stack trace`() {
        store.add(event(1, "A", attempts = 9, correlationId = "corr-1"))
        publisher.failOn(1)

        relay.runCycle()

        val error = logs.list.single { it.level == Level.ERROR }
        assertThat(error.formattedMessage).contains("eventId=${eventId(1)}", "aggregateId=A", "attempts=10")
        assertThat(error.throwableProxy.message).isEqualTo("sns down for 1")
        assertThat(error.mdcPropertyMap).containsEntry(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY, "corr-1")
        assertThat(MDC.get(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY)).isNull()
    }

    @Test
    fun `each publish failure increments the failure counter`() {
        store.add(event(1, "A"), event(2, "B"))
        publisher.failOn(1)
        publisher.failOn(2)

        relay.runCycle()

        assertThat(registry.get("outbox.publish.failures").counter().count()).isEqualTo(2.0)
    }

    @Test
    fun `a database error when marking an event published propagates instead of counting as a publish failure`() {
        store.add(event(1, "A"))
        store.failMarkPublished = true

        assertThatThrownBy { relay.runCycle() }.hasMessageContaining("database down")

        assertThat(store.failures).isEmpty()
        assertThat(registry.get("outbox.publish.failures").counter().count()).isZero()
    }

    @Test
    fun `stops publishing when the cycle exceeds its maximum duration and warns once`() {
        store.add(event(1, "A"), event(2, "B"), event(3, "C"), event(4, "D"))
        publisher.onPublish = { clock.advance(Duration.ofSeconds(4)) }

        relay.runCycle()

        assertThat(publisher.attempted.map { it.position }).containsExactly(1, 2, 3)
        assertThat(store.publishedAt.keys).containsExactly(1L, 2L, 3L)
        assertThat(logs.list.filter { it.level == Level.WARN }).singleElement().satisfies({
            assertThat(it.formattedMessage).contains("maximum duration")
        })
    }

    @Test
    fun `without leadership nothing is read nor published`() {
        store.add(event(1, "A"))
        store.leader = false

        relay.runCycle()

        assertThat(store.fetchLimits).isEmpty()
        assertThat(publisher.attempted).isEmpty()
        assertThat(store.publishedAt).isEmpty()
    }

    @Test
    fun `backlog gauges and the cycle counter are refreshed after every cycle, with or without leadership`() {
        store.backlog = OutboxBacklog(pending = 4, oldestAge = Duration.ofSeconds(3))
        store.leader = false

        relay.runCycle()
        relay.runCycle()

        assertThat(store.backlogQueriedAt).containsExactly(now, now)
        assertThat(registry.get("outbox.pending").gauge().value()).isEqualTo(4.0)
        assertThat(registry.get("outbox.oldest.pending.age").gauge().value()).isEqualTo(3.0)
        assertThat(registry.get("outbox.relay.cycles").counter().count()).isEqualTo(2.0)
    }

    private fun relay(settings: OutboxRelaySettings) = OutboxRelay(store, publisher, settings, clock, OutboxMetrics(registry))

    private fun eventId(position: Long) = UUID(0, position)

    private fun event(
        position: Long,
        aggregateId: String,
        attempts: Int = 0,
        correlationId: String? = null,
    ) = PendingEvent(
        position = position,
        eventId = eventId(position),
        aggregateId = aggregateId,
        eventType = "LedgerEntryRecorded",
        eventVersion = 1,
        payloadJson = """{"n":$position}""",
        correlationId = correlationId,
        occurredAt = now.minusSeconds(10),
        attempts = attempts,
    )

    private class MutableClock(
        private var instant: Instant,
    ) : Clock() {
        fun advance(duration: Duration) {
            instant = instant.plus(duration)
        }

        override fun instant(): Instant = instant

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this
    }

    /** Follows the [OutboxStore.fetchPending] contract: an account with a failed event only offers that event. */
    private class FakeOutboxStore : OutboxStore {
        var leader = true
        var failMarkPublished = false
        var backlog = OutboxBacklog(pending = 0, oldestAge = null)
        private val rows = linkedMapOf<Long, PendingEvent>()
        private var insideLeadership = false
        val leaderLockKeys = mutableListOf<Long>()
        val fetchLimits = mutableListOf<Int>()
        val publishedAt = linkedMapOf<Long, Instant>()
        val failures = linkedMapOf<Long, String>()
        val backlogQueriedAt = mutableListOf<Instant>()
        val callsOutsideLeadership = mutableListOf<String>()

        fun add(vararg events: PendingEvent) {
            events.forEach { rows[it.position] = it }
        }

        override fun <T : Any> asLeader(
            lockKey: Long,
            block: () -> T,
        ): T? {
            leaderLockKeys += lockKey
            if (!leader) return null
            insideLeadership = true
            try {
                return block()
            } finally {
                insideLeadership = false
            }
        }

        override fun fetchPending(limit: Int): List<PendingEvent> {
            requireLeadership("fetchPending")
            fetchLimits += limit
            val pending = rows.values.filter { it.position !in publishedAt }.sortedBy { it.position }
            return pending
                .filter { event ->
                    pending.none { it.aggregateId == event.aggregateId && it.attempts > 0 && it.position < event.position }
                }.take(limit)
        }

        override fun markPublished(
            position: Long,
            at: Instant,
        ) {
            requireLeadership("markPublished")
            if (failMarkPublished) throw IllegalStateException("database down")
            publishedAt[position] = at
        }

        override fun markFailed(
            position: Long,
            error: String,
        ) {
            requireLeadership("markFailed")
            failures[position] = error
            rows.computeIfPresent(position) { _, row -> row.copy(attempts = row.attempts + 1) }
        }

        override fun backlog(now: Instant): OutboxBacklog {
            backlogQueriedAt += now
            return backlog
        }

        private fun requireLeadership(call: String) {
            if (!insideLeadership) callsOutsideLeadership += call
        }
    }

    private class FakeEventPublisher : EventPublisher {
        private val failing = mutableSetOf<Long>()
        var onPublish: () -> Unit = {}
        val attempted = mutableListOf<PendingEvent>()
        val published = mutableListOf<PendingEvent>()

        fun failOn(position: Long) {
            failing += position
        }

        override fun publish(event: PendingEvent) {
            attempted += event
            onPublish()
            if (event.position in failing) throw IllegalStateException("sns down for ${event.position}")
            published += event
        }
    }
}
