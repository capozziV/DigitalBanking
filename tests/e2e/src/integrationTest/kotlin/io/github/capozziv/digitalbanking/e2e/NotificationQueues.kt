package io.github.capozziv.digitalbanking.e2e

import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** A message as the outbox relay published it: raw body (the envelope), FIFO group and string message attributes. */
data class PublishedMessage(
    val body: String,
    val groupId: String,
    val attributes: Map<String, String>,
)

/**
 * The queue `notification` consumes for the events of `transfers` and its DLQ, both created by the LocalStack init
 * script of the local environment. The tests write to the queue directly to deliver a message again or to deliver one
 * that can never be processed, and read the DLQ to see where that one ended.
 */
class NotificationQueues(
    private val sqs: SqsClient,
) {
    data class DeadLetter(
        val body: String,
        val receiveCount: Int,
    )

    private val transfersQueueUrl = url(TRANSFERS_QUEUE)
    private val transfersDlqUrl = url(TRANSFERS_DLQ)

    /** Delivers [message] again, with its attributes, under a new [deduplicationId] (so SQS does not drop it). */
    fun sendToTransfersQueue(
        message: PublishedMessage,
        deduplicationId: String,
    ) {
        sqs.sendMessage {
            it
                .queueUrl(transfersQueueUrl)
                .messageBody(message.body)
                .messageGroupId(message.groupId)
                .messageDeduplicationId(deduplicationId)
                .messageAttributes(
                    message.attributes.mapValues { (_, value) ->
                        MessageAttributeValue
                            .builder()
                            .dataType("String")
                            .stringValue(value)
                            .build()
                    },
                )
        }
    }

    /** Sends [body] as it is, without attributes, in its own [groupId]. */
    fun sendRawToTransfersQueue(
        body: String,
        groupId: String,
    ) {
        sqs.sendMessage {
            it
                .queueUrl(transfersQueueUrl)
                .messageBody(body)
                .messageGroupId(groupId)
                .messageDeduplicationId(UUID.randomUUID().toString())
        }
    }

    /** Polls until the queue has nothing waiting, in flight or delayed, seen twice in a row. */
    fun awaitTransfersQueueDrained(timeout: Duration) {
        val deadline = Instant.now().plus(timeout)
        var emptyReadings = 0
        while (emptyReadings < 2) {
            emptyReadings = if (pending(transfersQueueUrl) == 0) emptyReadings + 1 else 0
            check(Instant.now().isBefore(deadline)) { "$TRANSFERS_QUEUE not drained after $timeout: ${pending(transfersQueueUrl)} left" }
            Thread.sleep(POLL.toMillis())
        }
    }

    /** Polls until at least [count] messages wait in the queue (nobody consuming it). */
    fun awaitTransfersQueueDepth(
        count: Int,
        timeout: Duration,
    ) {
        val deadline = Instant.now().plus(timeout)
        while (visible(transfersQueueUrl) < count) {
            check(Instant.now().isBefore(deadline)) {
                "$TRANSFERS_QUEUE has ${visible(transfersQueueUrl)} messages after $timeout, expected $count"
            }
            Thread.sleep(POLL.toMillis())
        }
    }

    fun transfersDlqDepth(): Int = pending(transfersDlqUrl)

    /** Polls the DLQ until the message of [groupId] is there; deletes it and returns its body and receive count. */
    fun awaitDeadLetter(
        groupId: String,
        timeout: Duration,
    ): DeadLetter {
        val deadline = Instant.now().plus(timeout)
        while (Instant.now().isBefore(deadline)) {
            val message =
                sqs
                    .receiveMessage {
                        it
                            .queueUrl(transfersDlqUrl)
                            .maxNumberOfMessages(10)
                            .waitTimeSeconds(1)
                            .visibilityTimeout(DLQ_INSPECTION_VISIBILITY_SECONDS)
                            .messageSystemAttributeNames(
                                MessageSystemAttributeName.MESSAGE_GROUP_ID,
                                MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT,
                            )
                    }.messages()
                    .firstOrNull { it.attributes()[MessageSystemAttributeName.MESSAGE_GROUP_ID] == groupId }
            if (message != null) {
                sqs.deleteMessage { it.queueUrl(transfersDlqUrl).receiptHandle(message.receiptHandle()) }
                return DeadLetter(
                    body = message.body(),
                    receiveCount = message.attributes().getValue(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT).toInt(),
                )
            }
        }
        error("no message of group $groupId in $TRANSFERS_DLQ after $timeout")
    }

    private fun visible(queueUrl: String): Int = attributes(queueUrl).getValue(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)

    private fun pending(queueUrl: String): Int = attributes(queueUrl).values.sum()

    private fun attributes(queueUrl: String): Map<QueueAttributeName, Int> =
        sqs
            .getQueueAttributes {
                it.queueUrl(queueUrl).attributeNames(
                    QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                    QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
                    QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED,
                )
            }.attributes()
            .mapValues { it.value.toInt() }

    private fun url(name: String): String = sqs.getQueueUrl { it.queueName(name) }.queueUrl()

    private companion object {
        const val TRANSFERS_QUEUE = "notification-transfers.fifo"
        const val TRANSFERS_DLQ = "notification-transfers-dlq.fifo"
        val POLL: Duration = Duration.ofMillis(250)

        /** Long enough to delete the message just read (visibility 0 expires its receipt handle at once). */
        const val DLQ_INSPECTION_VISIBILITY_SECONDS = 5
    }
}
