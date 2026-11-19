package io.github.capozziv.digitalbanking.transfers.adapters.persistence

import io.github.capozziv.digitalbanking.outbox.JdbcOutboxWriter
import io.github.capozziv.digitalbanking.outbox.OutboxMessage
import io.github.capozziv.digitalbanking.transfers.application.port.EventOutbox
import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.TransferEvent
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.UUID

/**
 * Maps each TransferEvent to its payload (spec, section 6); `TransferCompensated` is eventVersion 2, which added `reason` and writes it to the outbox in the
 * caller's transaction. The aggregate is the transfer; amounts are numbers of cents.
 */
@Component
class OutboxEventAppender(
    private val writer: JdbcOutboxWriter,
    private val objectMapper: ObjectMapper,
) : EventOutbox {
    override fun append(event: TransferEvent) {
        writer.write(
            OutboxMessage(
                eventType = event.eventType(),
                eventVersion = event.eventVersion(),
                aggregateId = event.transferId.toString(),
                payloadJson = objectMapper.writeValueAsString(event.payload()),
            ),
        )
    }

    private fun TransferEvent.eventType(): String =
        when (this) {
            is TransferEvent.TransferCompleted -> "TransferCompleted"
            is TransferEvent.TransferFailed -> "TransferFailed"
            is TransferEvent.TransferCompensated -> "TransferCompensated"
        }

    private fun TransferEvent.eventVersion(): Int =
        when (this) {
            is TransferEvent.TransferCompensated -> COMPENSATED_EVENT_VERSION
            else -> EVENT_VERSION
        }

    private fun TransferEvent.payload(): Map<String, Any?> =
        when (this) {
            is TransferEvent.TransferCompleted -> {
                payload(sourceAccountId, destinationAccountId, amount, description)
            }

            is TransferEvent.TransferCompensated -> {
                payload(sourceAccountId, destinationAccountId, amount, description) + ("reason" to reason)
            }

            is TransferEvent.TransferFailed -> {
                payload(sourceAccountId, destinationAccountId, amount, description) + ("reason" to reason)
            }
        }

    private fun TransferEvent.payload(
        source: UUID,
        destination: UUID,
        amount: Money,
        description: String?,
    ): Map<String, Any?> =
        linkedMapOf(
            "transferId" to transferId.toString(),
            "sourceAccountId" to source.toString(),
            "destinationAccountId" to destination.toString(),
            "amount" to amount.cents,
            "description" to description,
        )

    private companion object {
        const val EVENT_VERSION = 1

        /** Version 1 had no `reason`; from version 2 on it is always present, null when the saga compensated without a refusal. */
        const val COMPENSATED_EVENT_VERSION = 2
    }
}
