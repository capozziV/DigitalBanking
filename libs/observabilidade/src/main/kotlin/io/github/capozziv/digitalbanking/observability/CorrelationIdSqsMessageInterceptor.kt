package io.github.capozziv.digitalbanking.observability

import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor
import org.slf4j.MDC
import org.springframework.messaging.Message

/**
 * Puts the `correlationId` message attribute (set by the outbox relay) in the MDC for the whole processing of an SQS
 * message, so every line logged while consuming it carries the ID.
 *
 * The container logs a failed message (`Error processing message`, with the stack trace) only after the pipeline,
 * interceptors included, has finished. So after a failure the ID stays in the MDC for that line, and the next message
 * on the thread replaces or removes it in [intercept]; after a success it is removed right away. The container line
 * runs on the thread that completed the processing, in practice the one that ran the listener.
 */
class CorrelationIdSqsMessageInterceptor : MessageInterceptor<Any> {
    override fun intercept(message: Message<Any>): Message<Any> {
        val correlationId = message.headers[MESSAGE_ATTRIBUTE]?.toString()
        if (correlationId.isNullOrBlank()) MDC.remove(CorrelationId.MDC_KEY) else MDC.put(CorrelationId.MDC_KEY, correlationId)
        return message
    }

    override fun afterProcessing(
        message: Message<Any>,
        t: Throwable?,
    ) {
        if (t == null) MDC.remove(CorrelationId.MDC_KEY)
    }

    companion object {
        /** SQS message attribute (raw message delivery) where the outbox relay puts the correlation ID. */
        const val MESSAGE_ATTRIBUTE = "correlationId"
    }
}
