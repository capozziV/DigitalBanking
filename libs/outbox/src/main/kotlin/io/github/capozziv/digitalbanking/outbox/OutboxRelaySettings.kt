package io.github.capozziv.digitalbanking.outbox

/**
 * Relay configuration. [lockKey] is the PostgreSQL advisory lock that elects the single active relay
 * of a service; [alertAfterAttempts] is the attempt count from which a failure is logged as `ERROR`.
 */
data class OutboxRelaySettings(
    val batchSize: Int = 100,
    val alertAfterAttempts: Int = 10,
    val lockKey: Long,
) {
    init {
        require(batchSize > 0) { "batchSize must be positive" }
        require(alertAfterAttempts > 0) { "alertAfterAttempts must be positive" }
    }
}
