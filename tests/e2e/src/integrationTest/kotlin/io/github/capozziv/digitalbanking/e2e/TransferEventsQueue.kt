package io.github.capozziv.digitalbanking.e2e

import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName
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
    /**
     * Reads and deletes messages until each of [transferIds] has at least one event, then a little longer to catch
     * duplicates. Returns the event types received per transfer, in arrival order.
     */
    fun awaitEvents(
        transferIds: Set<String>,
        timeout: Duration,
    ): Map<String, List<String>> {
        val received = transferIds.associateWith { mutableListOf<String>() }
        val deadline = Instant.now().plus(timeout)
        while (Instant.now().isBefore(deadline) && received.values.any { it.isEmpty() }) {
            receive(1).forEach { (groupId, eventType) -> received[groupId]?.add(eventType) }
        }
        repeat(CONFIRMATION_POLLS) { receive(2).forEach { (groupId, eventType) -> received[groupId]?.add(eventType) } }
        return received
    }

    private fun receive(waitSeconds: Int): List<Pair<String, String>> {
        val messages =
            sqs
                .receiveMessage {
                    it
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(waitSeconds)
                        .messageSystemAttributeNames(MessageSystemAttributeName.MESSAGE_GROUP_ID)
                        .messageAttributeNames("All")
                }.messages()
        messages.forEach { message -> sqs.deleteMessage { it.queueUrl(queueUrl).receiptHandle(message.receiptHandle()) } }
        return messages.map { message ->
            message.attributes().getValue(MessageSystemAttributeName.MESSAGE_GROUP_ID) to
                message.messageAttributes().getValue(EVENT_TYPE_ATTRIBUTE).stringValue()
        }
    }

    private companion object {
        const val CONFIRMATION_POLLS = 2

        /** Message attribute set by the outbox relay (`SnsEventPublisher.EVENT_TYPE_ATTRIBUTE`). */
        const val EVENT_TYPE_ATTRIBUTE = "eventType"
    }
}
