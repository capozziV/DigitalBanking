package io.github.capozziv.digitalbanking.e2e

import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.time.Instant

/**
 * The FIFO inspection queue subscribed to `transfers-events.fifo` (raw message delivery), created by the LocalStack
 * init script of the local environment. Events are grouped by transfer (`MessageGroupId` = transfer id).
 */
class TransferEventsQueue(
    private val sqs: SqsClient,
    private val queueUrl: String,
) {
    /** An event as seen in the queue: its type and the correlation ID carried as message attribute and in the envelope. */
    data class ReceivedEvent(
        val eventType: String,
        val attributeCorrelationId: String?,
        val envelopeCorrelationId: String?,
    )

    /**
     * Reads and deletes messages until each of [transferIds] has at least one event, then a little longer to catch
     * duplicates. Returns the events received per transfer, in arrival order.
     */
    fun awaitEvents(
        transferIds: Set<String>,
        timeout: Duration,
    ): Map<String, List<ReceivedEvent>> {
        val received = transferIds.associateWith { mutableListOf<ReceivedEvent>() }
        val deadline = Instant.now().plus(timeout)
        while (Instant.now().isBefore(deadline) && received.values.any { it.isEmpty() }) {
            receive(1).forEach { (groupId, event) -> received[groupId]?.add(event) }
        }
        repeat(CONFIRMATION_POLLS) { receive(2).forEach { (groupId, event) -> received[groupId]?.add(event) } }
        return received
    }

    /**
     * Reads and deletes messages until the one of type [eventType] of transfer [transferId] arrives, and returns it as
     * published (body and message attributes). Messages of other transfers are discarded.
     */
    fun awaitMessage(
        transferId: String,
        eventType: String,
        timeout: Duration,
    ): PublishedMessage {
        val deadline = Instant.now().plus(timeout)
        while (Instant.now().isBefore(deadline)) {
            val messages = receiveRaw(1)
            messages.forEach { message -> sqs.deleteMessage { it.queueUrl(queueUrl).receiptHandle(message.receiptHandle()) } }
            messages
                .firstOrNull { message ->
                    message.attributes()[MessageSystemAttributeName.MESSAGE_GROUP_ID] == transferId &&
                        message.messageAttributes()[EVENT_TYPE_ATTRIBUTE]?.stringValue() == eventType
                }?.let { message ->
                    return PublishedMessage(
                        body = message.body(),
                        groupId = transferId,
                        attributes = message.messageAttributes().mapValues { it.value.stringValue() },
                    )
                }
        }
        error("no $eventType of transfer $transferId in the inspection queue after $timeout")
    }

    private fun receiveRaw(waitSeconds: Int): List<Message> =
        sqs
            .receiveMessage {
                it
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(waitSeconds)
                    .messageSystemAttributeNames(MessageSystemAttributeName.MESSAGE_GROUP_ID)
                    .messageAttributeNames("All")
            }.messages()

    private fun receive(waitSeconds: Int): List<Pair<String, ReceivedEvent>> {
        val messages = receiveRaw(waitSeconds)
        messages.forEach { message -> sqs.deleteMessage { it.queueUrl(queueUrl).receiptHandle(message.receiptHandle()) } }
        return messages.map { message ->
            message.attributes().getValue(MessageSystemAttributeName.MESSAGE_GROUP_ID) to
                ReceivedEvent(
                    eventType = message.messageAttributes().getValue(EVENT_TYPE_ATTRIBUTE).stringValue(),
                    attributeCorrelationId = message.messageAttributes()[CORRELATION_ID_ATTRIBUTE]?.stringValue(),
                    envelopeCorrelationId = envelopeCorrelationId(message.body()),
                )
        }
    }

    private fun envelopeCorrelationId(body: String): String? =
        mapper
            .readTree(body)
            .get("correlationId")
            ?.takeUnless { it.isNull }
            ?.asString()

    private companion object {
        val mapper: JsonMapper = JsonMapper.builder().build()
        const val CORRELATION_ID_ATTRIBUTE = "correlationId"

        const val CONFIRMATION_POLLS = 2

        /** Message attribute set by the outbox relay (`SnsEventPublisher.EVENT_TYPE_ATTRIBUTE`). */
        const val EVENT_TYPE_ATTRIBUTE = "eventType"
    }
}
