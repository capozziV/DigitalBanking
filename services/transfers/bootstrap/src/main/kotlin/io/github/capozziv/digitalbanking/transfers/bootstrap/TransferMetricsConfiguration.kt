package io.github.capozziv.digitalbanking.transfers.bootstrap

import io.github.capozziv.digitalbanking.transfers.adapters.persistence.JdbcPendingTransferStats
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.Duration

/**
 * Gauges of the pending transfers, read from the database on each scrape: `transfers.pending` and
 * `transfers.oldest.pending.age` (seconds since the oldest pending transfer was created). A transfer stuck in a loop,
 * such as a compensation refused again and again (a bug), shows up as an old pending transfer. Each gauge runs its
 * own query ([JdbcPendingTransferStats.pending]), so a scrape costs two queries.
 */
class TransferMetrics(
    registry: MeterRegistry,
    stats: JdbcPendingTransferStats,
    clock: Clock,
) {
    init {
        Gauge
            .builder("transfers.pending", stats) { it.pending().count.toDouble() }
            .description("Transfers not yet completed, failed or compensated")
            .register(registry)
        Gauge
            .builder("transfers.oldest.pending.age", stats) { s ->
                s.pending().oldestCreatedAt?.let { Duration.between(it, clock.instant()).toNanos() / NANOS_PER_SECOND } ?: 0.0
            }.description("Age of the oldest pending transfer, since its creation")
            .baseUnit("seconds")
            .register(registry)
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}

@Configuration(proxyBeanMethods = false)
class TransferMetricsConfiguration {
    @Bean
    fun transferMetrics(
        registry: MeterRegistry,
        stats: JdbcPendingTransferStats,
        clock: Clock,
    ) = TransferMetrics(registry, stats, clock)
}
