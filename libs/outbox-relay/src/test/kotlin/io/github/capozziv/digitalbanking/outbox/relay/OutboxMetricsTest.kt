package io.github.capozziv.digitalbanking.outbox.relay

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class OutboxMetricsTest {
    private val registry = SimpleMeterRegistry()
    private val metrics = OutboxMetrics(registry)

    @Test
    fun `gauges start at zero and the failure counter at zero`() {
        assertThat(registry.get("outbox.pending").gauge().value()).isZero()
        assertThat(registry.get("outbox.oldest.pending.age").gauge().value()).isZero()
        assertThat(registry.get("outbox.publish.failures").counter().count()).isZero()
        assertThat(registry.get("outbox.relay.cycles").counter().count()).isZero()
    }

    @Test
    fun `each relay cycle increments the cycle counter`() {
        metrics.recordCycle()

        assertThat(registry.get("outbox.relay.cycles").counter().count()).isEqualTo(1.0)
    }

    @Test
    fun `pending backlog updates the gauges, with the age in seconds`() {
        metrics.recordBacklog(OutboxBacklog(pending = 3, oldestAge = Duration.ofMillis(2500)))

        assertThat(registry.get("outbox.pending").gauge().value()).isEqualTo(3.0)
        assertThat(registry.get("outbox.oldest.pending.age").gauge().value()).isEqualTo(2.5)
        assertThat(
            registry
                .get("outbox.oldest.pending.age")
                .gauge()
                .id.baseUnit,
        ).isEqualTo("seconds")
    }

    @Test
    fun `an empty backlog resets the age to zero`() {
        metrics.recordBacklog(OutboxBacklog(pending = 3, oldestAge = Duration.ofSeconds(9)))
        metrics.recordBacklog(OutboxBacklog(pending = 0, oldestAge = null))

        assertThat(registry.get("outbox.pending").gauge().value()).isZero()
        assertThat(registry.get("outbox.oldest.pending.age").gauge().value()).isZero()
    }

    @Test
    fun `each publish failure increments the counter`() {
        metrics.recordPublishFailure()
        metrics.recordPublishFailure()

        assertThat(registry.get("outbox.publish.failures").counter().count()).isEqualTo(2.0)
    }
}
