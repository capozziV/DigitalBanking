package io.github.capozziv.digitalbanking.transfers.bootstrap

import io.github.capozziv.digitalbanking.transfers.adapters.persistence.JdbcPendingTransferStats
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.hibernate.validator.constraints.time.DurationMin
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated
import java.time.Clock
import java.time.Duration

/**
 * Gauges of the pending transfers, read from the database on each scrape: `transfers.pending`,
 * `transfers.oldest.pending.age` (seconds since the oldest pending transfer was created) and `transfers.pending.aged`
 * (pending transfers created more than [TransferMetricsProperties.pendingAgedAfter] ago). A transfer stuck in a loop,
 * such as a compensation refused again and again (a bug) or a machine token `accounts` never accepts, shows up as an
 * old pending transfer; the saga has no retry ceiling, so these gauges are the alarm. Each gauge runs its own query, so
 * a scrape costs three queries.
 */
class TransferMetrics(
    registry: MeterRegistry,
    stats: JdbcPendingTransferStats,
    clock: Clock,
    pendingAgedAfter: Duration,
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
        Gauge
            .builder("transfers.pending.aged", stats) { it.pendingCreatedBefore(clock.instant().minus(pendingAgedAfter)).toDouble() }
            .description("Pending transfers created more than ${pendingAgedAfter.seconds} seconds ago")
            .register(registry)
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}

/**
 * `transfers.metrics.*`. [pendingAgedAfter] (10 min by default, five times the saga deadline) is the age from which a
 * pending transfer counts on `transfers.pending.aged`.
 */
@ConfigurationProperties("transfers.metrics")
@Validated
data class TransferMetricsProperties(
    @field:DurationMin(nanos = 1)
    val pendingAgedAfter: Duration = Duration.ofMinutes(10),
)

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TransferMetricsProperties::class)
class TransferMetricsConfiguration {
    @Bean
    fun transferMetrics(
        registry: MeterRegistry,
        stats: JdbcPendingTransferStats,
        clock: Clock,
        properties: TransferMetricsProperties,
    ) = TransferMetrics(registry, stats, clock, properties.pendingAgedAfter)
}
