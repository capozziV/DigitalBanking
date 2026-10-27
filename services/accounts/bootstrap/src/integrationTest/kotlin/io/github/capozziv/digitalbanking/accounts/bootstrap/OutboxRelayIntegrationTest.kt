package io.github.capozziv.digitalbanking.accounts.bootstrap

import io.github.capozziv.digitalbanking.accounts.bootstrap.web.ApiClient
import io.github.capozziv.digitalbanking.outbox.PendingEvent
import io.github.capozziv.digitalbanking.outbox.relay.EventPublisher
import io.github.capozziv.digitalbanking.outbox.relay.OutboxMetrics
import io.github.capozziv.digitalbanking.outbox.relay.OutboxRelay
import io.github.capozziv.digitalbanking.outbox.relay.OutboxRelaySettings
import io.github.capozziv.digitalbanking.outbox.relay.OutboxStore
import io.github.capozziv.digitalbanking.outbox.relay.SnsEventPublisher
import io.github.capozziv.digitalbanking.testing.localstack.LocalStackTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
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
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Collections
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * The relay of the running application publishes to the SNS FIFO topic created by the local init script,
 * and a FIFO inspection queue (raw message delivery) receives the events of each account in order.
 */
@TestPropertySource(properties = ["accounts.outbox.relay-enabled=true", "accounts.outbox.poll-interval=100ms"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(OutboxRelayIntegrationTest.RecordingPublisherConfiguration::class)
class OutboxRelayIntegrationTest : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var store: OutboxStore

    @Autowired
    private lateinit var publisher: RecordingEventPublisher

    @Autowired
    private lateinit var settings: OutboxRelaySettings

    @Autowired
    private lateinit var clock: Clock

    @Autowired
    private lateinit var metrics: OutboxMetrics

    private val api by lazy { ApiClient(port) }

    /** Events left by other integration tests on the shared database are not part of these tests. */
    @BeforeEach
    fun skipBacklogOfOtherTests() {
        markPendingOutboxEventsPublished()
    }

    @Test
    fun `events of each account reach the inspection queue in sequence order with type and correlation id`() {
        val first = openAccount("relay-first")
        credit(first, 1_000, "relay-first")
        debit(first, 300, "relay-first")
        credit(first, 50, "relay-first")
        val second = openAccount("relay-second")
        credit(second, 200, "relay-second")

        val received = awaitEvents(mapOf(first to 4, second to 2))

        mapOf(first to "relay-first", second to "relay-second").forEach { (account, correlationId) ->
            val events = received.getValue(account)
            assertThat(events.map { it.eventType })
                .containsExactly("AccountOpened", *Array(events.size - 1) { "LedgerEntryRecorded" })
            assertThat(
                events.drop(1).map {
                    it.body
                        .get("payload")
                        .get("sequence")
                        .asLong()
                },
            ).containsExactlyElementsOf((1L..events.size - 1L).toList())
            assertThat(events).allSatisfy {
                assertThat(it.groupId).isEqualTo(account)
                assertThat(it.correlationId).isEqualTo(correlationId)
                assertThat(it.body.get("correlationId").asString()).isEqualTo(correlationId)
                assertThat(it.body.get("eventType").asString()).isEqualTo(it.eventType)
            }
        }
        assertThat(
            received.getValue(first).drop(1).map {
                it.body
                    .get("payload")
                    .get("kind")
                    .asString()
            },
        ).containsExactly("CREDIT", "DEBIT", "CREDIT")
    }

    @Test
    fun `two relays running in parallel publish each event once and in order`() {
        val running = AtomicBoolean(true)
        val relays = List(2) { OutboxRelay(store, publisher, settings, clock, metrics) }
        val cycles = List(relays.size) { AtomicInteger() }
        val loops = Executors.newFixedThreadPool(relays.size)
        val loopResults =
            relays.mapIndexed { i, relay ->
                loops.submit {
                    while (running.get()) {
                        relay.runCycle()
                        cycles[i].incrementAndGet()
                    }
                }
            }
        val accounts =
            try {
                val operations = Executors.newVirtualThreadPerTaskExecutor()
                val opened =
                    (1..CONCURRENT_ACCOUNTS)
                        .map { n ->
                            operations.submit<String> {
                                val account = openAccount("relay-parallel-$n")
                                repeat(CREDITS_PER_ACCOUNT) { credit(account, 10L + it, "relay-parallel-$n") }
                                account
                            }
                        }.map { it.get(60, TimeUnit.SECONDS) }
                operations.close()
                awaitNoPendingEvents(opened)
                opened
            } finally {
                running.set(false)
                loops.shutdown()
                assertThat(loops.awaitTermination(30, TimeUnit.SECONDS)).isTrue()
                // A cycle that threw ends its loop; get() rethrows it here instead of losing it.
                loopResults.forEach { it.get() }
            }
        assertThat(cycles.map { it.get() }).allSatisfy { assertThat(it).isPositive() }

        val expectedPerAccount = CREDITS_PER_ACCOUNT + 1
        accounts.forEach { account ->
            val published = publisher.published().filter { it.aggregateId == account }
            assertThat(published.map { it.eventId }).doesNotHaveDuplicates().hasSize(expectedPerAccount)
            assertThat(published.map { it.position }).isSorted()
        }
        val received = awaitEvents(accounts.associateWith { expectedPerAccount })
        accounts.forEach { account ->
            assertThat(
                received.getValue(account).drop(1).map {
                    it.body
                        .get("payload")
                        .get("sequence")
                        .asLong()
                },
            ).containsExactlyElementsOf((1L..CREDITS_PER_ACCOUNT.toLong()).toList())
        }
    }

    @Test
    fun `relay meters are exposed on the actuator`() {
        val deadline = Instant.now().plusSeconds(10)
        var cycles = 0.0
        while (cycles <= 0.0) {
            check(Instant.now().isBefore(deadline)) { "outbox.relay.cycles still 0 after 10 s" }
            val response = api.get("/actuator/metrics/outbox.relay.cycles")
            assertThat(response.status).isEqualTo(200)
            cycles =
                response.body!!
                    .get("measurements")
                    .get(0)
                    .get("value")
                    .asDouble()
            if (cycles <= 0.0) Thread.sleep(100)
        }

        assertThat(api.get("/actuator/metrics/outbox.pending").status).isEqualTo(200)
    }

    private fun openAccount(correlationId: String): String {
        val response =
            api.post(
                "/accounts",
                """{"customerId":"${UUID.randomUUID()}"}""",
                headers(correlationId),
            )
        assertThat(response.status).isEqualTo(201)
        return response.body!!.get("id").asString()
    }

    private fun credit(
        account: String,
        amount: Long,
        correlationId: String,
    ) = assertThat(api.post("/accounts/$account/credits", """{"amount":$amount}""", headers(correlationId)).status)
        .isEqualTo(201)

    private fun debit(
        account: String,
        amount: Long,
        correlationId: String,
    ) = assertThat(api.post("/accounts/$account/debits", """{"amount":$amount}""", headers(correlationId)).status)
        .isEqualTo(201)

    private fun headers(correlationId: String) =
        mapOf("Idempotency-Key" to UUID.randomUUID().toString(), "X-Correlation-Id" to correlationId)

    private fun awaitNoPendingEvents(accounts: List<String>) {
        val deadline = Instant.now().plus(DELIVERY_TIMEOUT)
        while (Instant.now().isBefore(deadline)) {
            val pending =
                ownerJdbc
                    .sql("select count(*) from outbox where published_at is null and aggregate_id in (:ids)")
                    .param("ids", accounts)
                    .query(Long::class.java)
                    .single()
            if (pending == 0L) return
            Thread.sleep(100)
        }
        error("outbox events of $accounts still pending after $DELIVERY_TIMEOUT")
    }

    /**
     * Reads (and deletes) the inspection queue until each account has its expected number of events, then
     * polls a little longer so that duplicates would show up. Messages of other accounts are discarded.
     */
    private fun awaitEvents(expected: Map<String, Int>): Map<String, List<ReceivedEvent>> {
        val received = expected.keys.associateWith { mutableListOf<ReceivedEvent>() }
        val deadline = Instant.now().plus(DELIVERY_TIMEOUT)
        while (Instant.now().isBefore(deadline) && expected.any { (k, n) -> received.getValue(k).size < n }) {
            receiveBatch(1).forEach { event -> received[event.groupId]?.add(event) }
        }
        repeat(CONFIRMATION_POLLS) { receiveBatch(2).forEach { event -> received[event.groupId]?.add(event) } }
        expected.forEach { (account, count) -> assertThat(received.getValue(account)).hasSize(count) }
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
        return messages.map { it.toReceivedEvent() }
    }

    private fun Message.toReceivedEvent() =
        ReceivedEvent(
            groupId = attributes().getValue(MessageSystemAttributeName.MESSAGE_GROUP_ID),
            eventType = messageAttributes().getValue(SnsEventPublisher.EVENT_TYPE_ATTRIBUTE).stringValue(),
            correlationId = messageAttributes()[SnsEventPublisher.CORRELATION_ID_ATTRIBUTE]?.stringValue(),
            body = mapper.readTree(body()),
        )

    data class ReceivedEvent(
        val groupId: String,
        val eventType: String,
        val correlationId: String?,
        val body: JsonNode,
    )

    /** Wraps the application's publisher to record every successful publication, by any relay. */
    class RecordingEventPublisher(
        private val delegate: EventPublisher,
    ) : EventPublisher {
        private val published = Collections.synchronizedList(mutableListOf<PendingEvent>())

        override fun publish(event: PendingEvent) {
            delegate.publish(event)
            published += event
        }

        fun published(): List<PendingEvent> = synchronized(published) { published.toList() }
    }

    @TestConfiguration
    class RecordingPublisherConfiguration {
        @Bean
        @Primary
        fun recordingEventPublisher(snsEventPublisher: SnsEventPublisher) = RecordingEventPublisher(snsEventPublisher)
    }

    companion object {
        private const val CONCURRENT_ACCOUNTS = 6
        private const val CREDITS_PER_ACCOUNT = 5
        private const val CONFIRMATION_POLLS = 2
        private const val TOPIC_NAME = "accounts-events.fifo"
        private const val INSPECTION_QUEUE = "accounts-events-inspection.fifo"
        private val DELIVERY_TIMEOUT: Duration = Duration.ofSeconds(60)
        private val mapper = JsonMapper.builder().build()

        private lateinit var localStack: LocalStackContainer
        private lateinit var sqs: SqsClient
        private lateinit var queueUrl: String

        /** Started here, not in a static initializer, so a missing token skips the class (or fails it on CI). */
        @JvmStatic
        @BeforeAll
        fun startLocalStack() {
            val initDir = Path.of(System.getProperty("accounts.localstack.init-dir"))
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
            registry.add("accounts.aws.endpoint") { localStack.endpoint.toString() }
            registry.add("accounts.aws.region") { localStack.region }
            registry.add("accounts.aws.topic-arn") { "arn:aws:sns:${localStack.region}:000000000000:$TOPIC_NAME" }
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
