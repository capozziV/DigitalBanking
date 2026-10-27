package io.github.capozziv.digitalbanking.transfers.adapters.persistence

import io.github.capozziv.digitalbanking.outbox.JdbcOutboxWriter
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.simple.JdbcClient
import java.time.Clock
import java.time.Duration

/**
 * The outbox lib has no Spring configuration of its own; the service declares its writer. It uses the application
 * [Clock] when the context has one, and otherwise a UTC clock ticking in microseconds, as timestamptz stores them.
 */
@Configuration(proxyBeanMethods = false)
class OutboxWriterConfiguration {
    @Bean
    fun jdbcOutboxWriter(
        jdbcClient: JdbcClient,
        clock: ObjectProvider<Clock>,
    ): JdbcOutboxWriter {
        val writerClock = clock.getIfAvailable { Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000)) }
        return JdbcOutboxWriter(jdbcClient, writerClock)
    }
}
