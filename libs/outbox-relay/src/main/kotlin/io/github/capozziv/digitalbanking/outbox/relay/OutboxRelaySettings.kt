package io.github.capozziv.digitalbanking.outbox.relay

import java.time.Duration

/**
 * Relay configuration.
 *
 * - [lockKey]: PostgreSQL advisory lock that elects the single active relay of a service. Each service uses
 *   its own constant key, distinct from every other advisory lock taken on the same database.
 * - [alertAfterAttempts]: attempt count at which a failure is logged as `ERROR`, once, as the event becomes stuck;
 *   later failures of it are logged as `WARN`.
 * - [retryBackoff], [maxRetryBackoff]: wait before retrying a failed event, [retryBackoff] x 2^(attempts - 1)
 *   capped at [maxRetryBackoff]. Kept in memory by the active relay, so a new leader retries at once.
 * - [publishTimeout]: limit of each publish call to the broker.
 * - [maxCycleDuration]: once exceeded, the cycle stops publishing and leaves the rest for the next one,
 *   so the leader transaction (and its connection) is never held for long.
 */
data class OutboxRelaySettings(
    val batchSize: Int = DEFAULT_BATCH_SIZE,
    val alertAfterAttempts: Int = DEFAULT_ALERT_AFTER_ATTEMPTS,
    val lockKey: Long,
    val publishTimeout: Duration = DEFAULT_PUBLISH_TIMEOUT,
    val maxCycleDuration: Duration = DEFAULT_MAX_CYCLE_DURATION,
    val retryBackoff: Duration = DEFAULT_RETRY_BACKOFF,
    val maxRetryBackoff: Duration = DEFAULT_MAX_RETRY_BACKOFF,
) {
    companion object {
        const val DEFAULT_BATCH_SIZE = 100
        const val DEFAULT_ALERT_AFTER_ATTEMPTS = 10
        val DEFAULT_PUBLISH_TIMEOUT: Duration = Duration.ofSeconds(2)
        val DEFAULT_MAX_CYCLE_DURATION: Duration = Duration.ofSeconds(10)
        val DEFAULT_RETRY_BACKOFF: Duration = Duration.ofSeconds(1)
        val DEFAULT_MAX_RETRY_BACKOFF: Duration = Duration.ofSeconds(30)
    }

    init {
        require(batchSize > 0) { "batchSize must be positive" }
        require(alertAfterAttempts > 0) { "alertAfterAttempts must be positive" }
        require(publishTimeout > Duration.ZERO) { "publishTimeout must be positive" }
        require(maxCycleDuration > Duration.ZERO) { "maxCycleDuration must be positive" }
        require(retryBackoff > Duration.ZERO) { "retryBackoff must be positive" }
        require(maxRetryBackoff >= retryBackoff) { "maxRetryBackoff must not be below retryBackoff" }
    }
}
