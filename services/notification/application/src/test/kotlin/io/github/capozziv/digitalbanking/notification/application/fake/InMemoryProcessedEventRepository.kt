package io.github.capozziv.digitalbanking.notification.application.fake

import io.github.capozziv.digitalbanking.notification.application.port.ProcessedEventRepository
import java.time.Instant
import java.util.UUID

class InMemoryProcessedEventRepository(
    private val transactions: ImmediateTransactionRunner,
) : ProcessedEventRepository {
    val processed = linkedMapOf<UUID, String>()

    override fun markProcessed(
        eventId: UUID,
        eventType: String,
        at: Instant,
    ): Boolean {
        check(transactions.open) { "markProcessed outside a transaction" }
        if (eventId in processed) return false
        processed[eventId] = eventType
        return true
    }
}
