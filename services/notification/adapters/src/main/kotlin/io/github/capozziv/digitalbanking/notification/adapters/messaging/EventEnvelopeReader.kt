package io.github.capozziv.digitalbanking.notification.adapters.messaging

import io.github.capozziv.digitalbanking.notification.application.ReceivedEvent
import org.springframework.stereotype.Component
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

/** The message body is not an event envelope this service can read; the message must not be acknowledged. */
class MalformedEventException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Reads the outbox envelope (eventId, eventType, eventVersion, …, payload) published by accounts and transfers
 * and decodes the payload of the event types this service notifies about. Only the JSON is shared: no type of
 * the producers is used. Amounts are integer numbers of cents.
 */
@Component
class EventEnvelopeReader {
    private val jsonMapper = JsonMapper.builder().build()

    fun read(body: String): ReceivedEvent {
        val envelope =
            try {
                jsonMapper.readTree(body)
            } catch (e: JacksonException) {
                throw MalformedEventException("message body is not JSON", e)
            }
        if (envelope == null || !envelope.isObject) throw MalformedEventException("message body is not a JSON object")
        val eventId = envelope.uuid("eventId")
        val eventType = envelope.text("eventType")
        return when (eventType) {
            "AccountOpened" -> {
                val payload = envelope.payload()
                ReceivedEvent.AccountOpened(
                    eventId = eventId,
                    accountId = payload.uuid("accountId"),
                    customerId = payload.uuid("customerId"),
                    accountType = payload.text("type"),
                )
            }

            "AccountTypeChanged" -> {
                val payload = envelope.payload()
                ReceivedEvent.AccountTypeChanged(
                    eventId = eventId,
                    accountId = payload.uuid("accountId"),
                    newType = payload.text("newType"),
                    // Version 1 did not carry the limit; from version 2 on it is required.
                    overdraftLimitCents = if (envelope.version() >= 2) payload.cents("overdraftLimit") else null,
                )
            }

            "TransferCompleted" -> {
                val payload = envelope.payload()
                ReceivedEvent.TransferCompleted(
                    eventId = eventId,
                    transferId = payload.uuid("transferId"),
                    sourceAccountId = payload.uuid("sourceAccountId"),
                    destinationAccountId = payload.uuid("destinationAccountId"),
                    amountCents = payload.cents("amount"),
                )
            }

            "TransferFailed" -> {
                val payload = envelope.payload()
                ReceivedEvent.TransferFailed(
                    eventId = eventId,
                    transferId = payload.uuid("transferId"),
                    sourceAccountId = payload.uuid("sourceAccountId"),
                    destinationAccountId = payload.uuid("destinationAccountId"),
                    amountCents = payload.cents("amount"),
                    reason = payload.text("reason"),
                )
            }

            "TransferCompensated" -> {
                val payload = envelope.payload()
                ReceivedEvent.TransferCompensated(
                    eventId = eventId,
                    transferId = payload.uuid("transferId"),
                    sourceAccountId = payload.uuid("sourceAccountId"),
                    destinationAccountId = payload.uuid("destinationAccountId"),
                    amountCents = payload.cents("amount"),
                    // Version 1 did not carry the reason; from version 2 on it is present, null without a refusal.
                    reason = if (envelope.version() >= 2) payload.optionalText("reason") else null,
                )
            }

            "AccountBlocked" -> {
                val payload = envelope.payload()
                ReceivedEvent.AccountBlocked(eventId, payload.uuid("accountId"), payload.uuid("customerId"))
            }

            "AccountUnblocked" -> {
                val payload = envelope.payload()
                ReceivedEvent.AccountUnblocked(eventId, payload.uuid("accountId"), payload.uuid("customerId"))
            }

            "AccountClosed" -> {
                val payload = envelope.payload()
                ReceivedEvent.AccountClosed(eventId, payload.uuid("accountId"), payload.uuid("customerId"))
            }

            else -> {
                ReceivedEvent.Unsupported(eventId, eventType)
            }
        }
    }

    private fun JsonNode.payload(): JsonNode =
        get("payload")?.takeIf { it.isObject } ?: throw MalformedEventException("envelope has no payload object")

    private fun JsonNode.version(): Int =
        get("eventVersion")?.takeIf { it.isIntegralNumber }?.asInt()
            ?: throw MalformedEventException("envelope has no integer eventVersion")

    private fun JsonNode.text(field: String): String =
        get(field)?.takeIf { it.isString && it.asString().isNotBlank() }?.asString()
            ?: throw MalformedEventException("field '$field' is missing or not a string")

    private fun JsonNode.optionalText(field: String): String? {
        val node = get(field)
        return when {
            node == null || node.isNull -> null
            node.isString && node.asString().isNotBlank() -> node.asString()
            else -> throw MalformedEventException("field '$field' is not a string")
        }
    }

    private fun JsonNode.uuid(field: String): UUID {
        val value = text(field)
        return try {
            UUID.fromString(value)
        } catch (e: IllegalArgumentException) {
            throw MalformedEventException("field '$field' is not a UUID", e)
        }
    }

    private fun JsonNode.cents(field: String): Long =
        get(field)?.takeIf { it.isIntegralNumber && it.canConvertToLong() }?.asLong()
            ?: throw MalformedEventException("field '$field' is missing or not an integer number of cents")
}
