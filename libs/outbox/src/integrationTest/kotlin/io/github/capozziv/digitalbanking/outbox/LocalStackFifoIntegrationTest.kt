package io.github.capozziv.digitalbanking.outbox

import io.github.capozziv.digitalbanking.testing.localstack.LocalStackTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.localstack.LocalStackContainer
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.time.Instant

/**
 * Checks that the pinned LocalStack image behaves like AWS for SNS FIFO -> SQS FIFO:
 * order per MessageGroupId, deduplication by MessageDeduplicationId and message attributes
 * carried in the SNS envelope (RawMessageDelivery=false).
 */
class LocalStackFifoIntegrationTest {
    private val jsonMapper = JsonMapper.builder().build()

    @Test
    fun `fifo topic delivers to fifo queue in order per group, without duplicates, with attributes`() {
        val topicArn = sns.createTopic { it.name("t.fifo").attributes(mapOf("FifoTopic" to "true")) }.topicArn()
        val queueUrl =
            sqs
                .createQueue {
                    it.queueName("q.fifo").attributes(mapOf(QueueAttributeName.FIFO_QUEUE to "true"))
                }.queueUrl()
        val queueArn =
            sqs
                .getQueueAttributes { it.queueUrl(queueUrl).attributeNames(QueueAttributeName.QUEUE_ARN) }
                .attributes()
                .getValue(QueueAttributeName.QUEUE_ARN)
        sns.subscribe {
            it
                .topicArn(topicArn)
                .protocol("sqs")
                .endpoint(queueArn)
                .attributes(mapOf("RawMessageDelivery" to "false"))
        }

        listOf("A1" to "A", "B1" to "B", "A2" to "A", "B2" to "B", "A3" to "A", "A2" to "A")
            .forEach { (body, group) -> publish(topicArn, body, group) }

        val received = drain(queueUrl, expected = 5)

        assertThat(received.map { it.body }).containsExactlyInAnyOrder("A1", "A2", "A3", "B1", "B2")
        assertThat(received.filter { it.group == "A" }.map { it.body }).containsExactly("A1", "A2", "A3")
        assertThat(received.filter { it.group == "B" }.map { it.body }).containsExactly("B1", "B2")
        assertThat(received).allSatisfy {
            assertThat(it.eventType).isEqualTo("TestEvent")
            assertThat(it.correlationId).isEqualTo("corr-${it.body}")
        }
    }

    private fun publish(
        topicArn: String,
        body: String,
        group: String,
    ) {
        sns.publish {
            it
                .topicArn(topicArn)
                .message(body)
                .messageGroupId(group)
                .messageDeduplicationId("dedup-$body")
                .messageAttributes(
                    mapOf(
                        "eventType" to stringAttribute("TestEvent"),
                        "correlationId" to stringAttribute("corr-$body"),
                    ),
                )
        }
    }

    private fun stringAttribute(value: String) =
        MessageAttributeValue
            .builder()
            .dataType("String")
            .stringValue(value)
            .build()

    /**
     * Waits up to [deliveryTimeout] for [expected] messages, then polls [CONFIRMATION_POLLS] more times
     * so that a duplicate or extra message still in the queue is caught. Empty polls before the
     * expected messages arrive do not end the wait.
     */
    private fun drain(
        queueUrl: String,
        expected: Int,
        deliveryTimeout: Duration = Duration.ofSeconds(30),
    ): List<ReceivedMessage> {
        val received = mutableListOf<ReceivedMessage>()
        val deadline = Instant.now().plus(deliveryTimeout)
        while (received.size < expected && Instant.now().isBefore(deadline)) {
            received += receiveAndDelete(queueUrl, waitSeconds = 5)
        }
        repeat(CONFIRMATION_POLLS) { received += receiveAndDelete(queueUrl, waitSeconds = 2) }
        return received
    }

    private fun receiveAndDelete(
        queueUrl: String,
        waitSeconds: Int,
    ): List<ReceivedMessage> =
        sqs
            .receiveMessage {
                it
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(waitSeconds)
                    .messageSystemAttributeNamesWithStrings("MessageGroupId")
            }.messages()
            .map { message ->
                sqs.deleteMessage { it.queueUrl(queueUrl).receiptHandle(message.receiptHandle()) }
                parse(message)
            }

    private fun parse(message: Message): ReceivedMessage {
        val envelope = jsonMapper.readTree(message.body())
        val attributes = envelope.path("MessageAttributes")
        return ReceivedMessage(
            body = envelope.path("Message").asString(),
            group = message.attributesAsStrings().getValue("MessageGroupId"),
            eventType = attributes.path("eventType").path("Value").asString(),
            correlationId = attributes.path("correlationId").path("Value").asString(),
        )
    }

    private data class ReceivedMessage(
        val body: String,
        val group: String,
        val eventType: String,
        val correlationId: String,
    )

    companion object {
        private const val CONFIRMATION_POLLS = 2

        private lateinit var localStack: LocalStackContainer
        private lateinit var sns: SnsClient
        private lateinit var sqs: SqsClient

        @JvmStatic
        @BeforeAll
        fun startLocalStack() {
            localStack = LocalStackTestSupport.container(listOf("sns", "sqs"))
            localStack.start()
            sns = LocalStackTestSupport.snsClient(localStack)
            sqs = LocalStackTestSupport.sqsClient(localStack)
        }

        @JvmStatic
        @AfterAll
        fun stopLocalStack() {
            if (::sns.isInitialized) sns.close()
            if (::sqs.isInitialized) sqs.close()
            if (::localStack.isInitialized) localStack.stop()
        }
    }
}
