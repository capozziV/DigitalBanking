package io.github.capozziv.digitalbanking.notification.bootstrap

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DlqDepthMonitorTest {
    private val depths = mutableMapOf("accounts-dlq" to 3.0, "transfers-dlq" to 0.0)
    private val probed = mutableListOf<String>()
    private val monitor =
        DlqDepthMonitor(listOf("accounts-dlq", "transfers-dlq")) { queue ->
            probed += queue
            depths.getValue(queue)
        }
    private val registry = SimpleMeterRegistry().also { monitor.bindTo(it) }

    private fun gauge(queue: String): Double =
        registry
            .get("notification.dlq.messages")
            .tag("queue", queue)
            .gauge()
            .value()

    @Test
    fun `reading the gauge never calls SQS`() {
        repeat(3) { gauge("accounts-dlq") }

        assertThat(probed).isEmpty()
    }

    @Test
    fun `before the first refresh the depth is unknown`() {
        assertThat(gauge("accounts-dlq")).isNaN()
    }

    @Test
    fun `a refresh reads each DLQ once and the gauge shows the stored value`() {
        monitor.refresh()

        assertThat(probed).containsExactly("accounts-dlq", "transfers-dlq")
        assertThat(gauge("accounts-dlq")).isEqualTo(3.0)
        assertThat(gauge("transfers-dlq")).isEqualTo(0.0)
        assertThat(probed).hasSize(2)
    }

    @Test
    fun `the gauge keeps the value of the last refresh`() {
        monitor.refresh()
        depths["accounts-dlq"] = 7.0

        assertThat(gauge("accounts-dlq")).isEqualTo(3.0)
        monitor.refresh()
        assertThat(gauge("accounts-dlq")).isEqualTo(7.0)
    }

    @Test
    fun `a failed read is stored as unknown instead of keeping a stale depth`() {
        monitor.refresh()
        depths["accounts-dlq"] = Double.NaN

        monitor.refresh()

        assertThat(gauge("accounts-dlq")).isNaN()
        assertThat(gauge("transfers-dlq")).isEqualTo(0.0)
    }
}
