package io.github.capozziv.digitalbanking.outbox.relay

import io.github.capozziv.digitalbanking.outbox.OutboxEventEnvelope
import io.github.capozziv.digitalbanking.outbox.PendingEvent
import io.github.capozziv.digitalbanking.testing.localstack.LocalStackTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.localstack.LocalStackContainer
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.SnsException
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** Publishes through a FIFO topic into a FIFO queue subscribed with raw message delivery, as consumers will. */
class SnsEventPublisherIntegrationTest {
    @Test
    fun `publishes the envelope as body with group, deduplication id and attributes`() {
        val (topicArn, queueUrl) = topicWithQueue("full")
        val event = event(aggregateId = "acc-1", correlationId = "corr-1")

        SnsEventPublisher(sns, topicArn, PUBLISH_TIMEOUT).publish(event)

        val message = receiveOne(queueUrl)
        assertThat(message.body()).isEqualTo(OutboxEventEnvelope.toJson(event))
        assertThat(message.attributes())
            .containsEntry(MessageSystemAttributeName.MESSAGE_GROUP_ID, "acc-1")
            .containsEntry(MessageSystemAttributeName.MESSAGE_DEDUPLICATION_ID, event.eventId.toString())
        assertThat(message.messageAttributes().mapValues { it.value.stringValue() })
            .containsExactlyInAnyOrderEntriesOf(mapOf("eventType" to "LedgerEntryRecorded", "correlationId" to "corr-1"))
    }

    @Test
    fun `omits the correlation id attribute when the event has none`() {
        val (topicArn, queueUrl) = topicWithQueue("nocorr")

        SnsEventPublisher(sns, topicArn, PUBLISH_TIMEOUT).publish(event(aggregateId = "acc-2", correlationId = null))

        assertThat(receiveOne(queueUrl).messageAttributes().keys).containsExactly("eventType")
    }

    @Test
    fun `throws when the topic does not accept the message`() {
        val missingTopic = "arn:aws:sns:${localStack.region}:000000000000:missing.fifo"

        assertThatThrownBy {
            SnsEventPublisher(
                sns,
                missingTopic,
                PUBLISH_TIMEOUT,
            ).publish(event(aggregateId = "acc-3", correlationId = null))
        }.isInstanceOf(SnsException::class.java)
    }

    private fun event(
        aggregateId: String,
        correlationId: String?,
    ) = PendingEvent(
        position = 1,
        eventId = UUID.randomUUID(),
        aggregateId = aggregateId,
        eventType = "LedgerEntryRecorded",
        eventVersion = 1,
        payloadJson = """{"amount": 3700}""",
        correlationId = correlationId,
        occurredAt = Instant.parse("2026-10-22T14:31:00.123456Z"),
        attempts = 0,
    )

    private fun topicWithQueue(name: String): Pair<String, String> {
        val topicArn = sns.createTopic { it.name("$name.fifo").attributes(mapOf("FifoTopic" to "true")) }.topicArn()
        val queueUrl =
            sqs
                .createQueue { it.queueName("$name.fifo").attributes(mapOf(QueueAttributeName.FIFO_QUEUE to "true")) }
                .queueUrl()
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
                .attributes(mapOf("RawMessageDelivery" to "true"))
        }
        return topicArn to queueUrl
    }

    private fun receiveOne(queueUrl: String): Message =
        sqs
            .receiveMessage {
                it
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(10)
                    .messageAttributeNames("All")
                    .messageSystemAttributeNames(
                        MessageSystemAttributeName.MESSAGE_GROUP_ID,
                        MessageSystemAttributeName.MESSAGE_DEDUPLICATION_ID,
                    )
            }.messages()
            .single()

    companion object {
        private val PUBLISH_TIMEOUT = Duration.ofSeconds(5)
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
