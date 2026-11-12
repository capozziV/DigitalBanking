package io.github.capozziv.digitalbanking.transfers.adapters.dailylimit

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Daily limit of transfers per source account, in cents ([amount]), and how long the Redis counter lives without a
 * reservation ([counterTtl]) before it is rebuilt from PostgreSQL.
 */
@ConfigurationProperties("transfers.daily-limit")
data class DailyLimitProperties(
    val amount: Long = 500_000,
    val counterTtl: Duration = Duration.ofMinutes(10),
) {
    init {
        require(amount > 0) { "transfers.daily-limit.amount must be positive, was $amount" }
        require(counterTtl.seconds > 0) { "transfers.daily-limit.counter-ttl must be at least one second, was $counterTtl" }
    }
}
