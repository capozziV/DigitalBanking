package io.github.capozziv.digitalbanking.accounts.adapters.persistence

import io.github.capozziv.digitalbanking.accounts.application.port.EventOutbox
import io.github.capozziv.digitalbanking.accounts.domain.AccountEvent
import io.github.capozziv.digitalbanking.accounts.domain.AccountOpened
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeChanged
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntryRecorded
import io.github.capozziv.digitalbanking.outbox.JdbcOutboxWriter
import io.github.capozziv.digitalbanking.outbox.OutboxMessage
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.format.DateTimeFormatter

/**
 * Maps each AccountEvent to its payload (spec, section 5) and eventVersion (1; AccountTypeChanged is 2, with overdraftLimit) and writes it to the outbox in the
 * caller's transaction. Amounts are numbers of cents; instants are ISO-8601 UTC strings.
 */
@Component
class OutboxEventAppender(
    private val writer: JdbcOutboxWriter,
    private val objectMapper: ObjectMapper,
) : EventOutbox {
    override fun append(event: AccountEvent) {
        writer.write(
            OutboxMessage(
                eventType = event.eventType(),
                eventVersion = event.eventVersion(),
                aggregateId = event.accountId.toString(),
                payloadJson = objectMapper.writeValueAsString(event.payload()),
            ),
        )
    }

    private fun AccountEvent.eventType(): String =
        when (this) {
            is AccountOpened -> "AccountOpened"
            is LedgerEntryRecorded -> "LedgerEntryRecorded"
            is AccountTypeChanged -> "AccountTypeChanged"
        }

    private fun AccountEvent.eventVersion(): Int =
        when (this) {
            is AccountOpened -> 1
            is LedgerEntryRecorded -> 1
            is AccountTypeChanged -> 2
        }

    private fun AccountEvent.payload(): Map<String, Any?> =
        when (this) {
            is AccountOpened -> {
                linkedMapOf(
                    "accountId" to accountId.toString(),
                    "customerId" to customerId.toString(),
                    "type" to type.name,
                )
            }

            is LedgerEntryRecorded -> {
                linkedMapOf(
                    "entryId" to entry.id.toString(),
                    "accountId" to entry.accountId.toString(),
                    "sequence" to entry.sequence,
                    "kind" to entry.kind.name,
                    "amount" to entry.amount.cents,
                    "balanceAfter" to entry.balanceAfter.cents,
                    "description" to entry.description,
                    "idempotencyKey" to entry.idempotencyKey,
                    "createdAt" to DateTimeFormatter.ISO_INSTANT.format(entry.createdAt),
                )
            }

            is AccountTypeChanged -> {
                linkedMapOf(
                    "accountId" to accountId.toString(),
                    "previousType" to previousType.name,
                    "newType" to newType.name,
                    "overdraftLimit" to overdraftLimit.cents,
                )
            }
        }
}
