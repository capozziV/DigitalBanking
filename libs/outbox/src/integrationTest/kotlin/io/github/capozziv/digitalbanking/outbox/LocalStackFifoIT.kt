package io.github.capozziv.digitalbanking.outbox

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import tools.jackson.databind.json.JsonMapper

/**
 * Checks that the pinned LocalStack image behaves like AWS for SNS FIFO -> SQS FIFO:
 * order per MessageGroupId, deduplication by MessageDeduplicationId and message attributes
 * carried in the SNS envelope (RawMessageDelivery=false).
 */
class LocalStackFifoIT {
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

    /** Reads until [expected] messages arrive, then one more long poll to prove nothing else is left. */
    private fun drain(
        queueUrl: String,
        expected: Int,
    ): List<ReceivedMessage> {
        val received = mutableListOf<ReceivedMessage>()
        var emptyPolls = 0
        while (emptyPolls < 2 && received.size <= expected) {
            val messages =
                sqs
                    .receiveMessage {
                        it
                            .queueUrl(queueUrl)
                            .maxNumberOfMessages(10)
                            .waitTimeSeconds(if (received.size < expected) 5 else 2)
                            .messageSystemAttributeNamesWithStrings("MessageGroupId")
                    }.messages()
            if (messages.isEmpty()) emptyPolls++
            messages.forEach { message ->
                received += parse(message.body(), message.attributesAsStrings().getValue("MessageGroupId"))
                sqs.deleteMessage { it.queueUrl(queueUrl).receiptHandle(message.receiptHandle()) }
            }
        }
        return received
    }

    private fun parse(
        snsEnvelope: String,
        group: String,
    ): ReceivedMessage {
        val envelope = jsonMapper.readTree(snsEnvelope)
        val attributes = envelope.path("MessageAttributes")
        return ReceivedMessage(
            body = envelope.path("Message").asString(),
            group = group,
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
        /** Pinned release; keep in sync with the LocalStack image used by the compose and CI. */
        const val LOCALSTACK_IMAGE = "localstack/localstack:2026.10.2"
        private const val AUTH_TOKEN_VARIABLE = "LOCALSTACK_AUTH_TOKEN"

        private lateinit var localStack: LocalStackContainer
        private lateinit var sns: SnsClient
        private lateinit var sqs: SqsClient

        @JvmStatic
        @BeforeAll
        fun startLocalStack() {
            val authToken = System.getenv(AUTH_TOKEN_VARIABLE)
            assumeTrue(
                !authToken.isNullOrBlank(),
                "$AUTH_TOKEN_VARIABLE is not set; skipping LocalStack tests (load infra/local/.env into the environment)",
            )
            localStack =
                LocalStackContainer(DockerImageName.parse(LOCALSTACK_IMAGE))
                    .withServices("sns", "sqs")
                    .withEnv(AUTH_TOKEN_VARIABLE, authToken)
                    .withOptionalEgressProxy()
            localStack.start()
            val credentials =
                StaticCredentialsProvider.create(AwsBasicCredentials.create(localStack.accessKey, localStack.secretKey))
            val region = Region.of(localStack.region)
            sns =
                SnsClient
                    .builder()
                    .endpointOverride(localStack.endpoint)
                    .region(region)
                    .credentialsProvider(credentials)
                    .build()
            sqs =
                SqsClient
                    .builder()
                    .endpointOverride(localStack.endpoint)
                    .region(region)
                    .credentialsProvider(credentials)
                    .build()
        }

        /**
         * LocalStack must reach its licensing server at startup. Where outbound HTTPS only works through
         * a proxy that re-terminates TLS, set LOCALSTACK_HTTPS_PROXY (reachable from the container) and
         * LOCALSTACK_CA_BUNDLE (host path of the proxy CA). Unset, nothing changes.
         */
        private fun LocalStackContainer.withOptionalEgressProxy(): LocalStackContainer {
            val proxy = System.getenv("LOCALSTACK_HTTPS_PROXY")?.takeIf { it.isNotBlank() } ?: return this
            withEnv("HTTPS_PROXY", proxy)
            withEnv("NO_PROXY", "localhost,127.0.0.1")
            System.getenv("LOCALSTACK_CA_BUNDLE")?.takeIf { it.isNotBlank() }?.let { caBundle ->
                withCopyFileToContainer(MountableFile.forHostPath(caBundle), CONTAINER_CA_BUNDLE)
                withEnv("REQUESTS_CA_BUNDLE", CONTAINER_CA_BUNDLE)
                withEnv("SSL_CERT_FILE", CONTAINER_CA_BUNDLE)
            }
            return this
        }

        private const val CONTAINER_CA_BUNDLE = "/etc/ssl/certs/egress-proxy-ca.crt"

        @JvmStatic
        @AfterAll
        fun stopLocalStack() {
            if (::sns.isInitialized) sns.close()
            if (::sqs.isInitialized) sqs.close()
            if (::localStack.isInitialized) localStack.stop()
        }
    }
}
