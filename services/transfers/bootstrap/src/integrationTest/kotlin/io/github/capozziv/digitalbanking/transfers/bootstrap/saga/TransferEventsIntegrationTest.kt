package io.github.capozziv.digitalbanking.transfers.bootstrap.saga

import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.github.capozziv.digitalbanking.outbox.relay.SnsEventPublisher
import io.github.capozziv.digitalbanking.testing.localstack.LocalStackTestSupport
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs.Companion.entryCreated
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs.Companion.problem
import io.github.capozziv.digitalbanking.transfers.bootstrap.web.ApiClient
import org.assertj.core.api.Assertions.assertThat
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
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * The relay of the running application publishes the terminal events of the transfers to the SNS FIFO topic created
 * by the local init script; the FIFO inspection queue (raw message delivery) receives them, grouped by transfer and
 * with the correlation ID of the request.
 */
@TestPropertySource(properties = ["transfers.outbox.relay-enabled=true", "transfers.outbox.poll-interval=100ms"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TransferEventsIntegrationTest : TransfersIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }
    private val stubs = AccountsStubs(accountsServer)

    @BeforeEach
    fun reset() {
        accountsServer.resetAll()
        ownerJdbc.deleteTransfers()
    }

    @Test
    fun `completed, failed and compensated transfers reach the inspection queue with type, payload and correlation id`() {
        val completed =
            transfer("corr-events-completed") { source, destination ->
                accountsServer.stubFor(post(urlEqualTo("/accounts/$source/debits")).willReturn(entryCreated()))
                accountsServer.stubFor(post(urlEqualTo("/accounts/$destination/credits")).willReturn(entryCreated()))
            }
        val failed =
            transfer("corr-events-failed") { source, _ ->
                accountsServer.stubFor(
                    post(urlEqualTo("/accounts/$source/debits")).willReturn(problem(422, "insufficient-available-balance")),
                )
            }
        val compensated =
            transfer("corr-events-compensated") { source, destination ->
                accountsServer.stubFor(post(urlEqualTo("/accounts/$source/debits")).willReturn(entryCreated()))
                accountsServer.stubFor(post(urlEqualTo("/accounts/$destination/credits")).willReturn(problem(404, "account-not-found")))
                accountsServer.stubFor(post(urlEqualTo("/accounts/$source/credits")).willReturn(entryCreated()))
            }

        val received = awaitEvents(setOf(completed.id, failed.id, compensated.id))

        mapOf(
            completed to "TransferCompleted",
            failed to "TransferFailed",
            compensated to "TransferCompensated",
        ).forEach { (sent, eventType) ->
            val event = received.getValue(sent.id)
            assertThat(event.eventType).isEqualTo(eventType)
            assertThat(event.correlationId).isEqualTo(sent.correlationId)
            assertThat(event.body.get("eventType").asString()).isEqualTo(eventType)
            assertThat(event.body.get("eventVersion").asInt()).isEqualTo(1)
            assertThat(event.body.get("aggregateId").asString()).isEqualTo(sent.id)
            assertThat(event.body.get("correlationId").asString()).isEqualTo(sent.correlationId)
            val payload = event.body.get("payload")
            assertThat(payload.get("transferId").asString()).isEqualTo(sent.id)
            assertThat(payload.get("sourceAccountId").asString()).isEqualTo(sent.source.toString())
            assertThat(payload.get("destinationAccountId").asString()).isEqualTo(sent.destination.toString())
            assertThat(payload.get("amount").asLong()).isEqualTo(AMOUNT)
            assertThat(payload.get("description").asString()).isEqualTo("events")
        }
        assertThat(
            received
                .getValue(failed.id)
                .body
                .get("payload")
                .get("reason")
                .asString(),
        ).isEqualTo("insufficient-available-balance")
    }

    private data class SentTransfer(
        val id: String,
        val source: UUID,
        val destination: UUID,
        val correlationId: String,
    )

    private fun transfer(
        correlationId: String,
        stub: (UUID, UUID) -> Unit,
    ): SentTransfer {
        val source = UUID.randomUUID()
        val destination = UUID.randomUUID()
        stubs.accountExists(source)
        stubs.accountExists(destination)
        stub(source, destination)
        val response =
            api.post(
                "/transfers",
                """{"sourceAccountId":"$source","destinationAccountId":"$destination","amount":$AMOUNT,"description":"events"}""",
                mapOf("Idempotency-Key" to UUID.randomUUID().toString(), "X-Correlation-Id" to correlationId),
            )
        assertThat(response.status).describedAs(response.rawBody).isIn(200, 201, 422)
        val id = response.body!!.get("id")?.asString() ?: response.body.get("transferId").asString()
        return SentTransfer(id, source, destination, correlationId)
    }

    /** Reads (and deletes) the inspection queue until each transfer has one event, then a little longer for duplicates. */
    private fun awaitEvents(transferIds: Set<String>): Map<String, ReceivedEvent> {
        val received = transferIds.associateWith { mutableListOf<ReceivedEvent>() }
        val deadline = Instant.now().plus(DELIVERY_TIMEOUT)
        while (Instant.now().isBefore(deadline) && received.values.any { it.isEmpty() }) {
            receiveBatch(1).forEach { event -> received[event.groupId]?.add(event) }
        }
        repeat(CONFIRMATION_POLLS) { receiveBatch(2).forEach { event -> received[event.groupId]?.add(event) } }
        received.forEach { (id, events) -> assertThat(events).describedAs("events of transfer $id").hasSize(1) }
        return received.mapValues { it.value.single() }
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
        return messages.map { it.toReceivedEvent() }
    }

    private fun Message.toReceivedEvent() =
        ReceivedEvent(
            groupId = attributes().getValue(MessageSystemAttributeName.MESSAGE_GROUP_ID),
            eventType = messageAttributes().getValue(SnsEventPublisher.EVENT_TYPE_ATTRIBUTE).stringValue(),
            correlationId = messageAttributes()[SnsEventPublisher.CORRELATION_ID_ATTRIBUTE]?.stringValue(),
            body = mapper.readTree(body()),
        )

    private data class ReceivedEvent(
        val groupId: String,
        val eventType: String,
        val correlationId: String?,
        val body: JsonNode,
    )

    companion object {
        private const val AMOUNT = 4_200L
        private const val CONFIRMATION_POLLS = 2
        private const val TOPIC_NAME = "transfers-events.fifo"
        private const val INSPECTION_QUEUE = "transfers-events-inspection.fifo"
        private val DELIVERY_TIMEOUT: Duration = Duration.ofSeconds(60)
        private val mapper = JsonMapper.builder().build()

        private lateinit var localStack: LocalStackContainer
        private lateinit var sqs: SqsClient
        private lateinit var queueUrl: String

        /** Started here, not in a static initializer, so a missing token skips the class (or fails it on CI). */
        @JvmStatic
        @BeforeAll
        fun startLocalStack() {
            val initDir = Path.of(System.getProperty("transfers.localstack.init-dir"))
            localStack = LocalStackTestSupport.container(listOf("sns", "sqs"))
            Files.list(initDir).use { scripts ->
                scripts.forEach { script ->
                    localStack.withCopyFileToContainer(
                        MountableFile.forHostPath(script, 0b111_101_101),
                        "/etc/localstack/init/ready.d/${script.fileName}",
                    )
                }
            }
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
            registry.add("transfers.aws.endpoint") { localStack.endpoint.toString() }
            registry.add("transfers.aws.region") { localStack.region }
            registry.add("transfers.aws.topic-arn") { "arn:aws:sns:${localStack.region}:000000000000:$TOPIC_NAME" }
        }

        /** Waits for the ready.d scripts and fails if any of them did not succeed. */
        private fun awaitInitScripts() {
            val http = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder(URI.create("${localStack.endpoint}/_localstack/init/ready")).build()
            val deadline = Instant.now().plusSeconds(60)
            while (Instant.now().isBefore(deadline)) {
                val status = mapper.readTree(http.send(request, HttpResponse.BodyHandlers.ofString()).body())
                if (status.get("completed").asBoolean()) {
                    val scripts = status.get("scripts")
                    val states = (0 until scripts.size()).map { scripts.get(it).get("state").asString() }
                    check(states.isNotEmpty() && states.all { it == "SUCCESSFUL" }) {
                        "LocalStack init scripts did not succeed: $status\n${localStack.logs}"
                    }
                    return
                }
                Thread.sleep(250)
            }
            error("LocalStack init scripts did not complete:\n${localStack.logs}")
        }
    }
}
