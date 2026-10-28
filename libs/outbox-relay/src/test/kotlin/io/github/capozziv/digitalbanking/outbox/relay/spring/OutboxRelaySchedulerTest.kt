package io.github.capozziv.digitalbanking.outbox.relay.spring

import io.github.capozziv.digitalbanking.outbox.PendingEvent
import io.github.capozziv.digitalbanking.outbox.relay.EventPublisher
import io.github.capozziv.digitalbanking.outbox.relay.OutboxBacklog
import io.github.capozziv.digitalbanking.outbox.relay.OutboxMetrics
import io.github.capozziv.digitalbanking.outbox.relay.OutboxRelay
import io.github.capozziv.digitalbanking.outbox.relay.OutboxRelaySettings
import io.github.capozziv.digitalbanking.outbox.relay.OutboxStore
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class OutboxRelaySchedulerTest {
    private val store = BlockingStore()
    private val relay =
        OutboxRelay(
            store = store,
            publisher = NoOpPublisher(),
            settings = OutboxRelaySettings(lockKey = 1),
            clock = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneOffset.UTC),
            metrics = OutboxMetrics(SimpleMeterRegistry()),
        )

    @Test
    fun `shutdown waits for the cycle in progress to finish`() {
        val scheduler = OutboxRelayScheduler(relay, shutdownTimeout = Duration.ofSeconds(5))
        store.blockNextCycle()
        val cycle = Thread.ofPlatform().start { scheduler.runCycle() }
        assertThat(store.cycleEntered.await(5, TimeUnit.SECONDS)).isTrue()

        val shutdown = Thread.ofPlatform().start { scheduler.destroy().also { store.steps += "shutdown done" } }
        shutdown.join(300)
        assertThat(shutdown.isAlive).describedAs("shutdown returned while a cycle was running").isTrue()

        store.release.countDown()
        cycle.join(5_000)
        shutdown.join(5_000)
        assertThat(store.steps).containsExactly("cycle done", "shutdown done")
    }

    @Test
    fun `no cycle starts after shutdown`() {
        val scheduler = OutboxRelayScheduler(relay, shutdownTimeout = Duration.ofSeconds(5))

        scheduler.destroy()
        scheduler.runCycle()

        assertThat(store.leaderCalls.get()).isZero()
    }

    @Test
    fun `shutdown gives up after the timeout when a cycle does not finish`() {
        val scheduler = OutboxRelayScheduler(relay, shutdownTimeout = Duration.ofMillis(200))
        store.blockNextCycle()
        val cycle = Thread.ofPlatform().start { scheduler.runCycle() }
        assertThat(store.cycleEntered.await(5, TimeUnit.SECONDS)).isTrue()

        val started = System.nanoTime()
        scheduler.destroy()
        val waited = Duration.ofNanos(System.nanoTime() - started)

        assertThat(waited).isBetween(Duration.ofMillis(150), Duration.ofSeconds(3))
        store.release.countDown()
        cycle.join(5_000)
    }

    @Test
    fun `registers the cycle as a fixed delay task with the configured poll interval`() {
        val scheduler = OutboxRelayScheduler(relay, shutdownTimeout = Duration.ofSeconds(5), pollInterval = Duration.ofMillis(250))
        val registrar = ScheduledTaskRegistrar()

        scheduler.configureTasks(registrar)

        assertThat(registrar.fixedDelayTaskList).hasSize(1)
        assertThat(registrar.fixedDelayTaskList.single().intervalDuration).isEqualTo(Duration.ofMillis(250))
    }

    private class BlockingStore : OutboxStore {
        val leaderCalls = AtomicInteger()
        val steps: MutableList<String> = Collections.synchronizedList(mutableListOf())
        val cycleEntered = CountDownLatch(1)
        val release = CountDownLatch(1)

        @Volatile private var blockNext = false

        fun blockNextCycle() {
            blockNext = true
        }

        override fun <T : Any> asLeader(
            lockKey: Long,
            block: () -> T,
        ): T? {
            leaderCalls.incrementAndGet()
            if (blockNext) {
                cycleEntered.countDown()
                release.await(10, TimeUnit.SECONDS)
            }
            return block().also { steps += "cycle done" }
        }

        override fun fetchPending(limit: Int): List<PendingEvent> = emptyList()

        override fun markPublished(
            position: Long,
            at: Instant,
        ) = Unit

        override fun markFailed(
            position: Long,
            error: String,
        ) = Unit

        override fun backlog(now: Instant) = OutboxBacklog(pending = 0, oldestAge = null)
    }

    private class NoOpPublisher : EventPublisher {
        override fun publish(event: PendingEvent) = Unit
    }
}
