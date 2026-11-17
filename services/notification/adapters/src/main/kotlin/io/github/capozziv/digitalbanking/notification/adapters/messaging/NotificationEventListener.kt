package io.github.capozziv.digitalbanking.notification.adapters.messaging

import io.awspring.cloud.sqs.annotation.SqsListener
import io.github.capozziv.digitalbanking.notification.application.usecase.ConsumeEventUseCase
import io.github.capozziv.digitalbanking.notification.application.usecase.ConsumeOutcome
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Consumes the two FIFO queues (raw delivery: the body is the outbox envelope). The message is acknowledged only when
 * consumption returns, which is after the use case's transaction committed; any exception leaves it in the queue, and
 * the redrive policy sends it to the DLQ after the last receive.
 *
 * The correlation ID (`correlationId` message attribute) is put in the MDC by `CorrelationIdSqsMessageInterceptor`
 * (`libs/observabilidade`), which keeps it after a failure for the container's error line: this listener must not
 * clear it.
 */
@Component
class NotificationEventListener(
    private val reader: EventEnvelopeReader,
    private val consumeEvent: ConsumeEventUseCase,
) {
    @SqsListener(queueNames = [ACCOUNTS_QUEUE], acknowledgementMode = ON_SUCCESS)
    fun onAccountEvent(
        @Payload body: String,
    ) = consume(body)

    @SqsListener(queueNames = [TRANSFERS_QUEUE], acknowledgementMode = ON_SUCCESS)
    fun onTransferEvent(
        @Payload body: String,
    ) = consume(body)

    private fun consume(body: String) {
        try {
            val event = reader.read(body)
            when (consumeEvent.execute(event)) {
                ConsumeOutcome.PROCESSED -> log.info("event {} {} processed", event.eventType, event.eventId)
                ConsumeOutcome.DUPLICATE -> log.info("event {} {} already processed", event.eventType, event.eventId)
                ConsumeOutcome.IGNORED -> log.info("event {} {} ignored: type not notified", event.eventType, event.eventId)
            }
        } catch (e: Exception) {
            // Only the message: the container's ERROR line that follows carries the stack trace.
            log.warn("failed to consume message; it returns to the queue: {}", e.toString())
            throw e
        }
    }

    private companion object {
        const val ACCOUNTS_QUEUE = "\${notification.queues.accounts}"
        const val TRANSFERS_QUEUE = "\${notification.queues.transfers}"
        const val ON_SUCCESS = "ON_SUCCESS"

        val log = LoggerFactory.getLogger(NotificationEventListener::class.java)
    }
}
