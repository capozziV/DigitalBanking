package io.github.capozziv.digitalbanking.statement.adapters.messaging

import io.awspring.cloud.sqs.annotation.SqsListener
import io.github.capozziv.digitalbanking.outbox.OutboxEventEnvelope.CORRELATION_ID_MDC_KEY
import io.github.capozziv.digitalbanking.statement.application.ConflictingStatementEntryException
import io.github.capozziv.digitalbanking.statement.application.usecase.RecordOutcome
import io.github.capozziv.digitalbanking.statement.application.usecase.RecordStatementEntryUseCase
import io.github.capozziv.digitalbanking.statement.application.usecase.RegisterAccountUseCase
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Consumes the accounts FIFO queue (raw delivery: the body is the outbox envelope, `correlationId` is a message
 * attribute). The message is acknowledged only when consumption returns; any exception leaves it in the queue, and
 * the redrive policy sends it to the DLQ after the last receive. Both writes are idempotent, so a redelivery is a
 * no-op.
 */
@Component
class StatementEventListener(
    private val reader: EventEnvelopeReader,
    private val recordEntry: RecordStatementEntryUseCase,
    private val registerAccount: RegisterAccountUseCase,
) {
    @SqsListener(queueNames = [ACCOUNTS_QUEUE], acknowledgementMode = ON_SUCCESS)
    fun onAccountEvent(
        @Payload body: String,
        @Header(name = CORRELATION_ID_ATTRIBUTE, required = false) correlationId: String?,
    ) {
        if (correlationId != null) MDC.put(CORRELATION_ID_MDC_KEY, correlationId)
        try {
            consume(reader.read(body))
        } catch (e: ConflictingStatementEntryException) {
            // The ledger and the statement disagree: retrying cannot fix it, so it ends in the DLQ for inspection.
            log.error("conflicting statement entry; it returns to the queue", e)
            throw e
        } catch (e: Exception) {
            // Logged here, with the correlation ID still in the MDC: the container logs the error after the finally.
            log.warn("failed to consume message; it returns to the queue: {}", e.toString())
            throw e
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY)
        }
    }

    private fun consume(event: ReceivedEvent) {
        when (event) {
            is ReceivedEvent.LedgerEntryRecorded -> {
                val entry = event.entry
                when (recordEntry.execute(entry)) {
                    RecordOutcome.RECORDED -> log.info("entry {}/{} recorded", entry.accountId, entry.sequence)
                    RecordOutcome.DUPLICATE -> log.info("entry {}/{} already recorded", entry.accountId, entry.sequence)
                }
            }

            is ReceivedEvent.AccountOpened -> {
                registerAccount.execute(event.accountId, event.customerId)
                log.info("account {} registered", event.accountId)
            }

            is ReceivedEvent.Ignored -> {
                log.info("event {} v{} ignored", event.eventType, event.eventVersion)
            }
        }
    }

    private companion object {
        const val ACCOUNTS_QUEUE = "\${statement.queues.accounts}"
        const val ON_SUCCESS = "ON_SUCCESS"

        /** Message attribute set by the outbox relay (raw message delivery). */
        const val CORRELATION_ID_ATTRIBUTE = "correlationId"

        val log = LoggerFactory.getLogger(StatementEventListener::class.java)
    }
}
