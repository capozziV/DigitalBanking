package io.github.capozziv.digitalbanking.transfers.adapters.dailylimit

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Daily limit of transfers per source account, in cents ([amount]), and how long the Redis counter lives without a
 * reservation ([counterTtl]) before it is rebuilt from PostgreSQL. [lockTimeout] bounds the wait for the lock of the
 * account and day (then the reservation is unavailable and the saga retries); it must stay well inside the saga's call
 * budget. [circuitBreaker] sends reservations straight to PostgreSQL while Redis keeps failing.
 */
@ConfigurationProperties("transfers.daily-limit")
data class DailyLimitProperties(
    val amount: Long = 500_000,
    val counterTtl: Duration = Duration.ofMinutes(10),
    val lockTimeout: Duration = Duration.ofSeconds(2),
    val circuitBreaker: CircuitBreaker = CircuitBreaker(),
) {
    /** [failureThreshold] consecutive Redis failures open the circuit for [openDuration]. */
    data class CircuitBreaker(
        val failureThreshold: Int = 5,
        val openDuration: Duration = Duration.ofSeconds(10),
    ) {
        init {
            require(failureThreshold > 0) {
                "transfers.daily-limit.circuit-breaker.failure-threshold must be positive, was $failureThreshold"
            }
            require(!openDuration.isNegative && !openDuration.isZero) {
                "transfers.daily-limit.circuit-breaker.open-duration must be positive, was $openDuration"
            }
        }
    }

    init {
        require(amount > 0) { "transfers.daily-limit.amount must be positive, was $amount" }
        require(counterTtl.seconds > 0) { "transfers.daily-limit.counter-ttl must be at least one second, was $counterTtl" }
        require(lockTimeout.toMillis() > 0) { "transfers.daily-limit.lock-timeout must be at least one millisecond, was $lockTimeout" }
    }
}
