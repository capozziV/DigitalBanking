package io.github.capozziv.digitalbanking.transfers.adapters.dailylimit

import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitGateway
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.transaction.support.TransactionTemplate

/**
 * Wires the daily limit: the Redis counter with PostgreSQL as the source of truth and the fallback.
 *
 * The gateway's correctness depends on READ COMMITTED (the Spring Boot and PostgreSQL default, which the injected
 * [TransactionTemplate] keeps): after waiting for the advisory lock, each statement needs a fresh snapshot to see the
 * commit of whoever held it. Under REPEATABLE READ the snapshot is taken by the lock statement itself, before that
 * commit, and a reservation could miss the epoch and the reservations of the previous holder.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DailyLimitProperties::class)
class DailyLimitConfiguration {
    @Bean
    fun dailyLimitGateway(
        redis: StringRedisTemplate,
        jdbc: JdbcClient,
        transactions: TransactionTemplate,
        properties: DailyLimitProperties,
        meterRegistry: MeterRegistry,
    ): DailyLimitGateway =
        RedisDailyLimitGateway(redis, DailyLimitScripts(), JdbcDailyLimitStore(jdbc), transactions, properties, meterRegistry)
}
