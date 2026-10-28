package io.github.capozziv.digitalbanking.transfers.application.port

/**
 * Runs a block with the given correlation ID as the current one (logs, calls to `accounts`, outbox rows) and
 * restores the previous one afterwards. A null ID means none for the block.
 */
interface CorrelationContext {
    fun <T> withCorrelationId(
        correlationId: String?,
        block: () -> T,
    ): T
}
