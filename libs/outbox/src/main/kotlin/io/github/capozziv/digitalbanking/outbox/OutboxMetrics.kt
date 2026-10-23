package io.github.capozziv.digitalbanking.outbox

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Micrometer meters of the outbox relay: backlog gauges, the publish failure counter and the cycle counter.
 *
 * The gauges hold the value of the last cycle, so they freeze if the relay stops running; a stopped relay
 * shows as `outbox.relay.cycles` no longer increasing. Only completed cycles count: a cycle that throws
 * (a database error, for instance) records neither the cycle nor the backlog.
 */
class OutboxMetrics(
    registry: MeterRegistry,
) {
    private val pending = AtomicLong()
    private val oldestAgeSeconds = AtomicReference(0.0)
    private val publishFailures: Counter =
        Counter
            .builder("outbox.publish.failures")
            .description("Outbox events whose publication failed")
            .register(registry)
    private val cycles: Counter =
        Counter
            .builder("outbox.relay.cycles")
            .description("Relay cycles completed, with or without leadership")
            .register(registry)

    init {
        Gauge
            .builder("outbox.pending", pending) { it.get().toDouble() }
            .description("Outbox events waiting for publication")
            .register(registry)
        Gauge
            .builder("outbox.oldest.pending.age", oldestAgeSeconds) { it.get() }
            .description("Age of the oldest outbox event waiting for publication, as of the last relay cycle")
            .baseUnit("seconds")
            .register(registry)
    }

    fun recordBacklog(backlog: OutboxBacklog) {
        pending.set(backlog.pending)
        oldestAgeSeconds.set(backlog.oldestAge?.let { it.toNanos() / NANOS_PER_SECOND } ?: 0.0)
    }

    fun recordCycle() {
        cycles.increment()
    }

    fun recordPublishFailure() {
        publishFailures.increment()
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
