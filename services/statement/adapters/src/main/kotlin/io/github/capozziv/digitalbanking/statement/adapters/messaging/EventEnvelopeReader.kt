package io.github.capozziv.digitalbanking.statement.adapters.messaging

import io.github.capozziv.digitalbanking.statement.domain.EntryKind
import io.github.capozziv.digitalbanking.statement.domain.StatementEntry
import org.springframework.stereotype.Component
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID

/** An event read from the accounts queue, already decoded from its envelope. Money is in cents. */
sealed interface ReceivedEvent {
    data class LedgerEntryRecorded(
        val entry: StatementEntry,
    ) : ReceivedEvent

    data class AccountOpened(
        val accountId: UUID,
        val customerId: UUID,
    ) : ReceivedEvent

    /** An event type or version this service does not project; acknowledged without effect. */
    data class Ignored(
        val eventType: String,
        val eventVersion: Int,
    ) : ReceivedEvent
}

/** The message body is not an event envelope this service can read; the message must not be acknowledged. */
class MalformedEventException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Reads the outbox envelope (eventId, eventType, eventVersion, …, payload) published by accounts. Only the JSON is
 * shared: no type of the producer is used. Amounts are integer numbers of cents.
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
        val eventType = envelope.text("eventType")
        val version = envelope.version()
        if (version != SUPPORTED_VERSION) return ReceivedEvent.Ignored(eventType, version)
        return when (eventType) {
            "LedgerEntryRecorded" -> {
                ReceivedEvent.LedgerEntryRecorded(envelope.payload().toEntry())
            }

            "AccountOpened" -> {
                val payload = envelope.payload()
                ReceivedEvent.AccountOpened(accountId = payload.uuid("accountId"), customerId = payload.uuid("customerId"))
            }

            else -> {
                ReceivedEvent.Ignored(eventType, version)
            }
        }
    }

    private fun JsonNode.toEntry(): StatementEntry =
        try {
            StatementEntry(
                entryId = uuid("entryId"),
                accountId = uuid("accountId"),
                sequence = cents("sequence"),
                kind = EntryKind.valueOf(text("kind")),
                amountCents = cents("amount"),
                balanceAfterCents = cents("balanceAfter"),
                description = get("description")?.takeIf { !it.isNull }?.let { text("description") },
                createdAt = Instant.parse(text("createdAt")),
            )
        } catch (e: IllegalArgumentException) {
            throw MalformedEventException("ledger entry is invalid: ${e.message}", e)
        } catch (e: DateTimeParseException) {
            throw MalformedEventException("field 'createdAt' is not an instant", e)
        }

    private fun JsonNode.payload(): JsonNode =
        get("payload")?.takeIf { it.isObject } ?: throw MalformedEventException("envelope has no payload object")

    private fun JsonNode.version(): Int =
        get("eventVersion")?.takeIf { it.isIntegralNumber && it.canConvertToInt() }?.asInt()
            ?: throw MalformedEventException("envelope has no integer eventVersion")

    private fun JsonNode.text(field: String): String =
        get(field)?.takeIf { it.isString && it.asString().isNotBlank() }?.asString()
            ?: throw MalformedEventException("field '$field' is missing or not a string")

    private fun JsonNode.uuid(field: String): UUID =
        try {
            UUID.fromString(text(field))
        } catch (e: IllegalArgumentException) {
            throw MalformedEventException("field '$field' is not a UUID", e)
        }

    /** An integer number, for cents and for the ledger sequence. */
    private fun JsonNode.cents(field: String): Long =
        get(field)?.takeIf { it.isIntegralNumber && it.canConvertToLong() }?.asLong()
            ?: throw MalformedEventException("field '$field' is missing or not an integer number")

    private companion object {
        const val SUPPORTED_VERSION = 1
    }
}
