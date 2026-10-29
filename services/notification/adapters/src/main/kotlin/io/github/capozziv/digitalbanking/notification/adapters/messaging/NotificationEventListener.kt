package io.github.capozziv.digitalbanking.notification.adapters.messaging

import io.awspring.cloud.sqs.annotation.SqsListener
import io.github.capozziv.digitalbanking.notification.application.usecase.ConsumeEventUseCase
import io.github.capozziv.digitalbanking.notification.application.usecase.ConsumeOutcome
import io.github.capozziv.digitalbanking.outbox.OutboxEventEnvelope.CORRELATION_ID_MDC_KEY
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Consumes the two FIFO queues (raw delivery: the body is the outbox envelope, `correlationId` is a message
 * attribute). The message is acknowledged only when consumption returns, which is after the use case's
 * transaction committed; any exception leaves it in the queue, and the redrive policy sends it to the DLQ
 * after the last receive.
 */
@Component
class NotificationEventListener(
    private val reader: EventEnvelopeReader,
    private val consumeEvent: ConsumeEventUseCase,
) {
    @SqsListener(queueNames = [ACCOUNTS_QUEUE], acknowledgementMode = ON_SUCCESS)
    fun onAccountEvent(
        @Payload body: String,
        @Header(name = CORRELATION_ID_ATTRIBUTE, required = false) correlationId: String?,
    ) = consume(body, correlationId)

    @SqsListener(queueNames = [TRANSFERS_QUEUE], acknowledgementMode = ON_SUCCESS)
    fun onTransferEvent(
        @Payload body: String,
        @Header(name = CORRELATION_ID_ATTRIBUTE, required = false) correlationId: String?,
    ) = consume(body, correlationId)

    private fun consume(
        body: String,
        correlationId: String?,
    ) {
        if (correlationId != null) MDC.put(CORRELATION_ID_MDC_KEY, correlationId)
        try {
            val event = reader.read(body)
            when (consumeEvent.execute(event)) {
                ConsumeOutcome.PROCESSED -> log.debug("event {} {} processed", event.eventType, event.eventId)
                ConsumeOutcome.DUPLICATE -> log.debug("event {} {} already processed", event.eventType, event.eventId)
                ConsumeOutcome.IGNORED -> log.info("event {} {} ignored: type not notified", event.eventType, event.eventId)
            }
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY)
        }
    }

    private companion object {
        const val ACCOUNTS_QUEUE = "\${notification.queues.accounts}"
        const val TRANSFERS_QUEUE = "\${notification.queues.transfers}"
        const val ON_SUCCESS = "ON_SUCCESS"

        /** Message attribute set by the outbox relay (raw message delivery). */
        const val CORRELATION_ID_ATTRIBUTE = "correlationId"

        val log = LoggerFactory.getLogger(NotificationEventListener::class.java)
    }
}
