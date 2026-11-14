package io.github.capozziv.digitalbanking.customer.bootstrap

import io.github.capozziv.digitalbanking.customer.bootstrap.web.ApiClient
import io.github.capozziv.digitalbanking.outbox.relay.SnsEventPublisher
import io.github.capozziv.digitalbanking.testing.localstack.LocalStackTestSupport
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.awaitility.core.ConditionTimeoutException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.testcontainers.localstack.LocalStackContainer
import org.testcontainers.utility.MountableFile
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * The relay of the running application publishes to the SNS FIFO topic created by the local init script, and a FIFO
 * inspection queue (raw message delivery) receives the events of each customer in order.
 */
@TestPropertySource(properties = ["customer.outbox.relay-enabled=true", "customer.outbox.poll-interval=100ms"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OutboxRelayIntegrationTest : CustomerIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }

    /** Events left by other integration tests on the shared database are not part of this test. */
    @BeforeEach
    fun skipBacklogOfOtherTests() {
        markPendingOutboxEventsPublished()
    }

    @Test
    fun `the events of a customer reach the inspection queue in order with type and correlation id`() {
        val sub = UUID.randomUUID()
        val admin = JwtTestSupport.adminToken(UUID.randomUUID())
        val headers = mapOf("X-Correlation-Id" to "relay-customer")
        api.post("/customers", """{"name":"Ana Souza","email":"ana@example.test"}""", JwtTestSupport.customerToken(sub), headers)
        api.put("/admin/customers/$sub/status", """{"status":"BLOCKED"}""", admin, headers)
        api.put("/admin/customers/$sub/status", """{"status":"ACTIVE"}""", admin, headers)

        val events = awaitEvents(sub.toString(), 3)

        assertThat(events.map { it.eventType }).containsExactly("CustomerRegistered", "CustomerBlocked", "CustomerUnblocked")
        assertThat(events).allSatisfy {
            assertThat(it.groupId).isEqualTo(sub.toString())
            assertThat(it.correlationId).isEqualTo("relay-customer")
            assertThat(it.body.get("correlationId").asString()).isEqualTo("relay-customer")
            assertThat(it.body.get("eventType").asString()).isEqualTo(it.eventType)
            assertThat(
                it.body
                    .get("payload")
                    .get("customerId")
                    .asString(),
            ).isEqualTo(sub.toString())
        }
    }

    /** Reads (and deletes) the inspection queue until [expected] events of the customer arrived, then polls a little more for duplicates. */
    private fun awaitEvents(
        customerId: String,
        expected: Int,
    ): List<ReceivedEvent> {
        val received = mutableListOf<ReceivedEvent>()
        val deadline = Instant.now().plus(DELIVERY_TIMEOUT)
        while (Instant.now().isBefore(deadline) && received.size < expected) {
            received += receiveBatch(1).filter { it.groupId == customerId }
        }
        repeat(CONFIRMATION_POLLS) { received += receiveBatch(2).filter { it.groupId == customerId } }
        assertThat(received).hasSize(expected)
        return received
    }

    private fun receiveBatch(waitSeconds: Int): List<ReceivedEvent> {
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
        return messages.map {
            ReceivedEvent(
                groupId = it.attributes().getValue(MessageSystemAttributeName.MESSAGE_GROUP_ID),
                eventType = it.messageAttributes().getValue(SnsEventPublisher.EVENT_TYPE_ATTRIBUTE).stringValue(),
                correlationId = it.messageAttributes()[SnsEventPublisher.CORRELATION_ID_ATTRIBUTE]?.stringValue(),
                body = mapper.readTree(it.body()),
            )
        }
    }

    data class ReceivedEvent(
        val groupId: String,
        val eventType: String,
        val correlationId: String?,
        val body: JsonNode,
    )

    companion object {
        private const val CONFIRMATION_POLLS = 2
        private const val TOPIC_NAME = "customer-events.fifo"
        private const val INSPECTION_QUEUE = "customer-events-inspection.fifo"
        private val DELIVERY_TIMEOUT: Duration = Duration.ofSeconds(60)
        private val mapper = JsonMapper.builder().build()

        private lateinit var localStack: LocalStackContainer
        private lateinit var sqs: SqsClient
        private lateinit var queueUrl: String

        /** Started here, not in a static initializer, so a missing token skips the class (or fails it on CI). */
        @JvmStatic
        @BeforeAll
        fun startLocalStack() {
            val initDir = Path.of(System.getProperty("customer.localstack.init-dir"))
            localStack = LocalStackTestSupport.container(listOf("sns", "sqs"))
            // Only this service's script: mounting the whole folder breaks whenever another service adds one
            // that needs a LocalStack service this test does not start (04-statement.sh needs DynamoDB).
            val script = initDir.resolve("05-customer-topic.sh")
            localStack.withCopyFileToContainer(
                MountableFile.forHostPath(script, 0b111_101_101),
                "/etc/localstack/init/ready.d/${script.fileName}",
            )
            localStack.start()
            awaitInitScripts()
            sqs = LocalStackTestSupport.sqsClient(localStack)
            queueUrl = sqs.getQueueUrl { it.queueName(INSPECTION_QUEUE) }.queueUrl()
        }

        @JvmStatic
        @AfterAll
        fun stopLocalStack() {
            if (::sqs.isInitialized) sqs.close()
            if (::localStack.isInitialized) localStack.stop()
        }

        @JvmStatic
        @DynamicPropertySource
        fun awsProperties(registry: DynamicPropertyRegistry) {
            registry.add("customer.aws.endpoint") { localStack.endpoint.toString() }
            registry.add("customer.aws.region") { localStack.region }
            registry.add("customer.aws.topic-arn") { "arn:aws:sns:${localStack.region}:000000000000:$TOPIC_NAME" }
        }

        /** Waits for the ready.d script and fails if it did not succeed. */
        private fun awaitInitScripts() {
            val http = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder(URI.create("${localStack.endpoint}/_localstack/init/ready")).build()

            fun status() = mapper.readTree(http.send(request, HttpResponse.BodyHandlers.ofString()).body())
            try {
                await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofMillis(250)).until {
                    status().get("completed").asBoolean()
                }
            } catch (e: ConditionTimeoutException) {
                error("LocalStack init scripts did not complete:\n${localStack.logs}")
            }
            val status = status()
            val scripts = status.get("scripts")
            val states = (0 until scripts.size()).map { scripts.get(it).get("state").asString() }
            check(states.isNotEmpty() && states.all { it == "SUCCESSFUL" }) {
                "LocalStack init scripts did not succeed: $status\n${localStack.logs}"
            }
        }
    }
}
