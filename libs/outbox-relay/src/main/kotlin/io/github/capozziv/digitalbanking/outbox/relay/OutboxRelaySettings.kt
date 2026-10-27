package io.github.capozziv.digitalbanking.outbox.relay

import java.time.Duration

/**
 * Relay configuration.
 *
 * - [lockKey]: PostgreSQL advisory lock that elects the single active relay of a service. Each service uses
 *   its own constant key, distinct from every other advisory lock taken on the same database.
 * - [alertAfterAttempts]: attempt count from which a failure is logged as `ERROR`.
 * - [publishTimeout]: limit of each publish call to the broker.
 * - [maxCycleDuration]: once exceeded, the cycle stops publishing and leaves the rest for the next one,
 *   so the leader transaction (and its connection) is never held for long.
 */
data class OutboxRelaySettings(
    val batchSize: Int = 100,
    val alertAfterAttempts: Int = 10,
    val lockKey: Long,
    val publishTimeout: Duration = Duration.ofSeconds(2),
    val maxCycleDuration: Duration = Duration.ofSeconds(10),
) {
    init {
        require(batchSize > 0) { "batchSize must be positive" }
        require(alertAfterAttempts > 0) { "alertAfterAttempts must be positive" }
        require(publishTimeout > Duration.ZERO) { "publishTimeout must be positive" }
        require(maxCycleDuration > Duration.ZERO) { "maxCycleDuration must be positive" }
    }
}
