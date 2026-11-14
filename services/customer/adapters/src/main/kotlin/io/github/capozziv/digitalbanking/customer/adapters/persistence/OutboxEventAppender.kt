package io.github.capozziv.digitalbanking.customer.adapters.persistence

import io.github.capozziv.digitalbanking.customer.application.port.EventOutbox
import io.github.capozziv.digitalbanking.customer.domain.CustomerBlocked
import io.github.capozziv.digitalbanking.customer.domain.CustomerEvent
import io.github.capozziv.digitalbanking.customer.domain.CustomerRegistered
import io.github.capozziv.digitalbanking.customer.domain.CustomerUnblocked
import io.github.capozziv.digitalbanking.outbox.JdbcOutboxWriter
import io.github.capozziv.digitalbanking.outbox.OutboxMessage
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Maps each CustomerEvent to its payload (all events are version 1) and writes it to the outbox in the caller's
 * transaction. The aggregate id is the customer id, so the events of a customer keep their order on the FIFO topic.
 */
@Component
class OutboxEventAppender(
    private val writer: JdbcOutboxWriter,
    private val objectMapper: ObjectMapper,
) : EventOutbox {
    override fun append(event: CustomerEvent) {
        writer.write(
            OutboxMessage(
                eventType = event.eventType(),
                eventVersion = EVENT_VERSION,
                aggregateId = event.customerId.toString(),
                payloadJson = objectMapper.writeValueAsString(event.payload()),
            ),
        )
    }

    private fun CustomerEvent.eventType(): String =
        when (this) {
            is CustomerRegistered -> "CustomerRegistered"
            is CustomerBlocked -> "CustomerBlocked"
            is CustomerUnblocked -> "CustomerUnblocked"
        }

    private fun CustomerEvent.payload(): Map<String, Any?> =
        when (this) {
            is CustomerRegistered -> linkedMapOf("customerId" to customerId.toString(), "name" to name, "email" to email)
            is CustomerBlocked -> linkedMapOf("customerId" to customerId.toString())
            is CustomerUnblocked -> linkedMapOf("customerId" to customerId.toString())
        }

    private companion object {
        const val EVENT_VERSION = 1
    }
}
