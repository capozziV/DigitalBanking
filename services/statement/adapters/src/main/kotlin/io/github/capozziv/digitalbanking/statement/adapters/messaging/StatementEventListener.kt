package io.github.capozziv.digitalbanking.statement.adapters.messaging

import io.awspring.cloud.sqs.annotation.SqsListener
import io.github.capozziv.digitalbanking.statement.application.ConflictingStatementEntryException
import io.github.capozziv.digitalbanking.statement.application.usecase.RecordOutcome
import io.github.capozziv.digitalbanking.statement.application.usecase.RecordStatementEntryUseCase
import io.github.capozziv.digitalbanking.statement.application.usecase.RegisterAccountUseCase
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Consumes the accounts FIFO queue (raw delivery: the body is the outbox envelope). The message is acknowledged only
 * when consumption returns; any exception leaves it in the queue, and the redrive policy sends it to the DLQ after the
 * last receive. Both writes are idempotent, so a redelivery is a no-op.
 *
 * The correlation ID (`correlationId` message attribute) is put in the MDC by `CorrelationIdSqsMessageInterceptor`
 * (`libs/observabilidade`), which keeps it after a failure for the container's error line: this listener must not
 * clear it.
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
    ) {
        try {
            consume(reader.read(body))
        } catch (e: ConflictingStatementEntryException) {
            // The ledger and the statement disagree: retrying cannot fix it, so it ends in the DLQ for inspection.
            log.error("conflicting statement entry; it returns to the queue", e)
            throw e
        } catch (e: Exception) {
            // Only the message: the container's ERROR line that follows carries the stack trace.
            log.warn("failed to consume message; it returns to the queue: {}", e.toString())
            throw e
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

        val log = LoggerFactory.getLogger(StatementEventListener::class.java)
    }
}
