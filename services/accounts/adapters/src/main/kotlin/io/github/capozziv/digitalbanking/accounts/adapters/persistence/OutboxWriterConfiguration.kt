package io.github.capozziv.digitalbanking.accounts.adapters.persistence

import io.github.capozziv.digitalbanking.outbox.JdbcOutboxWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.simple.JdbcClient
import java.time.Clock

/** The outbox lib has no Spring configuration of its own; the service declares its writer. */
@Configuration(proxyBeanMethods = false)
class OutboxWriterConfiguration {
    @Bean
    fun jdbcOutboxWriter(jdbcClient: JdbcClient): JdbcOutboxWriter = JdbcOutboxWriter(jdbcClient, Clock.systemUTC())
}
