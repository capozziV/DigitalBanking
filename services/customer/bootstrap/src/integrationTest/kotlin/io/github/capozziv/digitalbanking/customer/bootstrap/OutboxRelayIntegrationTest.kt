package io.github.capozziv.digitalbanking.customer.bootstrap

import io.github.capozziv.digitalbanking.customer.bootstrap.web.ApiClient
import io.github.capozziv.digitalbanking.customer.bootstrap.web.assertProblem
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
import org.springframework.core.io.ClassPathResource
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.testcontainers.localstack.LocalStackContainer
import org.testcontainers.utility.MountableFile
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
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
        api.post(
            "/customers",
            """{"name":"Ana Souza","email":"ana@example.test","phone":"${nextPhone()}"}""",
            JwtTestSupport.customerToken(sub),
            headers,
        )
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

    @Test
    fun `the relay admin routes are served by the service, for admins only, in Problem Details`() {
        val admin = JwtTestSupport.adminToken(UUID.randomUUID())

        val stuck = api.get("/admin/outbox/stuck-events", admin)
        assertThat(stuck.status).describedAs(stuck.rawBody).isEqualTo(200)
        assertThat(stuck.body!!.isArray).isTrue()
        assertThat(api.get("/admin/outbox/stuck-events", JwtTestSupport.customerToken(UUID.randomUUID())).status).isEqualTo(403)

        val discard = api.post("/admin/outbox/events/${UUID.randomUUID()}/discard", """{"reason":"poison event"}""", admin)
        assertProblem(discard, 404, "outbox-event-not-found")
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

    /**
     * Issue 232, in the real order: the event is published while no queue is subscribed (the late queue does not exist
     * yet, so it never sees it), the queue is created and subscribed afterwards, and the V5 migration (run here as a
     * script, since the database is already migrated) republishes it through the relay to the late queue.
     */
    @Test
    fun `the V5 republication reaches a queue created after the original event was published`() {
        val sub = UUID.randomUUID()
        val phone = nextPhone()
        api.post(
            "/customers",
            """{"name":"Ana Souza","email":"ana@example.test","phone":"$phone"}""",
            JwtTestSupport.customerToken(sub),
        )
        awaitEvents(sub.toString(), 1) // the original reached the inspection queue, which already existed
        val lateQueueUrl = createLateQueueSubscribedToTheTopic()
        assertThat(receiveFrom(lateQueueUrl, 2).filter { it.groupId == sub.toString() })
            .describedAs("a queue created later never receives the original event")
            .isEmpty()

        val v5 = ClassPathResource("db/migration/V5__republish_customer_registered.sql").getContentAsString(Charsets.UTF_8)
        ownerJdbc.sql(v5).update()

        val republished = mutableListOf<ReceivedEvent>()
        val deadline = Instant.now().plus(DELIVERY_TIMEOUT)
        while (Instant.now().isBefore(deadline) && republished.none { it.groupId == sub.toString() }) {
            republished += receiveFrom(lateQueueUrl, 1)
        }
        val event = republished.single { it.groupId == sub.toString() }
        assertThat(event.eventType).isEqualTo("CustomerRegistered")
        assertThat(event.body.get("eventVersion").asInt()).isEqualTo(2)
        assertThat(event.body.get("aggregateId").asString()).isEqualTo(sub.toString())
        assertThat(
            event.body
                .get("payload")
                .get("phone")
                .asString(),
        ).isEqualTo(phone)
        assertThat(
            event.body
                .get("payload")
                .get("name")
                .asString(),
        ).isEqualTo("Ana Souza")
    }

    private fun createLateQueueSubscribedToTheTopic(): String {
        val url =
            sqs
                .createQueue {
                    it.queueName("late-notification-customer.fifo").attributes(
                        mapOf(
                            QueueAttributeName.FIFO_QUEUE to "true",
                            QueueAttributeName.CONTENT_BASED_DEDUPLICATION to "false",
                        ),
                    )
                }.queueUrl()
        val arn =
            sqs
                .getQueueAttributes { it.queueUrl(url).attributeNames(QueueAttributeName.QUEUE_ARN) }
                .attributes()
                .getValue(QueueAttributeName.QUEUE_ARN)
        sns.subscribe {
            it
                .topicArn("arn:aws:sns:${localStack.region}:000000000000:$TOPIC_NAME")
                .protocol("sqs")
                .endpoint(arn)
                .attributes(mapOf("RawMessageDelivery" to "true"))
        }
        return url
    }

    private fun receiveBatch(waitSeconds: Int): List<ReceivedEvent> = receiveFrom(queueUrl, waitSeconds)

    private fun receiveFrom(
        queueUrl: String,
        waitSeconds: Int,
    ): List<ReceivedEvent> {
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
        private lateinit var sns: SnsClient
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
            sns = LocalStackTestSupport.snsClient(localStack)
            queueUrl = sqs.getQueueUrl { it.queueName(INSPECTION_QUEUE) }.queueUrl()
        }

        @JvmStatic
        @AfterAll
        fun stopLocalStack() {
            if (::sqs.isInitialized) sqs.close()
            if (::sns.isInitialized) sns.close()
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
