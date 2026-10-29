package io.github.capozziv.digitalbanking.notification.bootstrap.messaging

import io.github.capozziv.digitalbanking.notification.bootstrap.NotificationApplication
import io.github.capozziv.digitalbanking.notification.bootstrap.NotificationIntegrationTest
import io.github.capozziv.digitalbanking.notification.bootstrap.web.ApiClient
import io.github.capozziv.digitalbanking.testing.localstack.LocalStackTestSupport
import io.micrometer.core.instrument.MeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.localstack.LocalStackContainer
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Consumption of both FIFO queues against LocalStack, with the redrive policy of production (5 receives) and a
 * visibility timeout of 1 s so retries are quick: redeliveries are no-ops, a malformed message ends in the DLQ
 * without holding other groups, and an event about an account not known yet is retried until it is.
 *
 * The queues are shared by the tests of the class, so each test compares the DLQs with their depth when it started.
 */
@SpringBootTest(classes = [NotificationApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(OutputCaptureExtension::class)
class NotificationConsumptionIntegrationTest {
    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    @LocalServerPort
    private var port: Int = 0

    private var accountsDlqAtStart = 0
    private var transfersDlqAtStart = 0

    @BeforeEach
    fun recordDlqDepths() {
        accountsDlqAtStart = visibleMessages(ACCOUNTS_DLQ)
        transfersDlqAtStart = visibleMessages(TRANSFERS_DLQ)
    }

    private val jdbc: JdbcClient by lazy {
        val postgres = NotificationIntegrationTest.postgres
        JdbcClient.create(DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password))
    }

    @Test
    fun `account opened and transfer completed notify each customer once`() {
        val (alice, aliceAccount) = UUID.randomUUID() to UUID.randomUUID()
        val (bob, bobAccount) = UUID.randomUUID() to UUID.randomUUID()

        sendAccountEvent(aliceAccount, accountOpened(aliceAccount, alice))
        sendAccountEvent(bobAccount, accountOpened(bobAccount, bob))
        val transferEventId = UUID.randomUUID()
        sendTransferEvent(transferEventId, transferCompleted(transferEventId, aliceAccount, bobAccount))

        await { kindsOf(alice).size == 2 && kindsOf(bob).size == 2 }
        assertThat(kindsOf(alice)).containsExactlyInAnyOrder("WELCOME", "TRANSFER_SENT")
        assertThat(kindsOf(bob)).containsExactlyInAnyOrder("WELCOME", "TRANSFER_RECEIVED")
        awaitEmpty(TRANSFERS_QUEUE)
    }

    @Test
    fun `the same transfer completed delivered 20 times notifies only once`() {
        val (alice, aliceAccount) = UUID.randomUUID() to UUID.randomUUID()
        val (bob, bobAccount) = UUID.randomUUID() to UUID.randomUUID()
        sendAccountEvent(aliceAccount, accountOpened(aliceAccount, alice))
        sendAccountEvent(bobAccount, accountOpened(bobAccount, bob))
        await { kindsOf(alice).size == 1 && kindsOf(bob).size == 1 }

        val transferEventId = UUID.randomUUID()
        val body = transferCompleted(transferEventId, aliceAccount, bobAccount)
        // Each send has its own deduplication ID, so SQS delivers all 20: the redelivery after a lost ack.
        repeat(20) { sendTransferEvent(transferEventId, body) }

        awaitEmpty(TRANSFERS_QUEUE)
        // The redeliveries were acknowledged as no-ops: none of them failed into the DLQ.
        assertThat(visibleMessages(TRANSFERS_DLQ)).isEqualTo(transfersDlqAtStart)
        assertThat(kindsOf(alice)).containsExactlyInAnyOrder("WELCOME", "TRANSFER_SENT")
        assertThat(kindsOf(bob)).containsExactlyInAnyOrder("WELCOME", "TRANSFER_RECEIVED")
        assertThat(notificationsOfEvent(transferEventId)).isEqualTo(2)
        assertThat(processedRows(transferEventId)).isEqualTo(1)
    }

    @Test
    fun `an unknown event type and a ledger entry recorded are acknowledged without notifications`() {
        val unknownEventId = UUID.randomUUID()
        val ledgerEventId = UUID.randomUUID()
        val accountId = UUID.randomUUID()
        sendAccountEvent(accountId, envelope(unknownEventId, "SomethingNew", """{"accountId":"$accountId"}"""))
        sendAccountEvent(
            accountId,
            envelope(
                ledgerEventId,
                "LedgerEntryRecorded",
                """{"entryId":"${UUID.randomUUID()}","accountId":"$accountId","kind":"CREDIT","amount":1000,""" +
                    """"balanceAfter":1000,"sequence":1}""",
            ),
        )

        await { processedRows(unknownEventId) == 1 && processedRows(ledgerEventId) == 1 }
        awaitEmpty(ACCOUNTS_QUEUE)
        assertThat(visibleMessages(ACCOUNTS_DLQ)).isEqualTo(accountsDlqAtStart)
        assertThat(notificationsOfEvent(unknownEventId)).isZero()
        assertThat(notificationsOfEvent(ledgerEventId)).isZero()
    }

    @Test
    fun `the correlation id of the SQS message attribute reaches the log of the consumption`(output: CapturedOutput) {
        val eventId = UUID.randomUUID()
        sendAccountEvent(UUID.randomUUID(), envelope(eventId, "SomethingNew", "{}"))

        await { output.out.lines().any { it.contains(eventId.toString()) && it.contains("ignored") } }
        val line = output.out.lines().first { it.contains(eventId.toString()) && it.contains("ignored") }
        // The envelope carries another id, so only the message attribute can put this one in the log.
        assertThat(line).contains("[$CORRELATION_ID]").doesNotContain(ENVELOPE_CORRELATION_ID)
    }

    @Test
    fun `the DLQ gauge is exposed by the metrics endpoint with the queue tag`() {
        val response = ApiClient(port).get("/actuator/metrics/notification.dlq.messages")

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        val tags = response.body!!.get("availableTags")
        val queueTag = (0 until tags.size()).map { tags.get(it) }.single { it.get("tag").asString() == "queue" }
        val values = (0 until queueTag.get("values").size()).map { queueTag.get("values").get(it).asString() }
        assertThat(values).containsExactlyInAnyOrder(ACCOUNTS_DLQ, TRANSFERS_DLQ)
    }

    @Test
    fun `a malformed message goes to the DLQ after 5 receives without holding other groups`() {
        val brokenGroup = "broken-${UUID.randomUUID()}"
        sqs.sendMessage {
            it
                .queueUrl(queueUrl(ACCOUNTS_QUEUE))
                .messageGroupId(brokenGroup)
                .messageDeduplicationId(UUID.randomUUID().toString())
                .messageBody("""{"eventId":"not-a-uuid","eventType":"AccountOpened"}""")
        }
        val (carol, carolAccount) = UUID.randomUUID() to UUID.randomUUID()
        sendAccountEvent(carolAccount, accountOpened(carolAccount, carol))

        // The other group is processed while the broken message is still being retried.
        await { kindsOf(carol) == listOf("WELCOME") }
        assertThat(visibleMessages(ACCOUNTS_DLQ)).isEqualTo(accountsDlqAtStart)

        await(Duration.ofSeconds(60)) { visibleMessages(ACCOUNTS_DLQ) == accountsDlqAtStart + 1 }
        awaitEmpty(ACCOUNTS_QUEUE)
        await { dlqGauge(ACCOUNTS_DLQ) == (accountsDlqAtStart + 1).toDouble() }
        assertThat(dlqGauge(TRANSFERS_DLQ)).isEqualTo(transfersDlqAtStart.toDouble())
        // SQS keeps the receive count when it moves the message, and the inspection is one more receive.
        assertThat(receiveCountInDlq(ACCOUNTS_DLQ, brokenGroup)).isEqualTo(MAX_RECEIVE_COUNT + 1)
    }

    @Test
    fun `a transfer of an account not known yet comes back until the account opened arrives`(output: CapturedOutput) {
        val (dave, daveAccount) = UUID.randomUUID() to UUID.randomUUID()
        val (erin, erinAccount) = UUID.randomUUID() to UUID.randomUUID()
        val transferEventId = UUID.randomUUID()

        sendTransferEvent(transferEventId, transferCompleted(transferEventId, daveAccount, erinAccount))
        // The transfer must fail at least once before its accounts are known: the listener error names the account.
        await { output.out.contains("unknown account $daveAccount") }
        assertThat(processedRows(transferEventId)).isZero()
        assertThat(notificationsOfEvent(transferEventId)).isZero()
        sendAccountEvent(daveAccount, accountOpened(daveAccount, dave))
        sendAccountEvent(erinAccount, accountOpened(erinAccount, erin))

        await { notificationsOfEvent(transferEventId) == 2 }
        assertThat(kindsOf(dave)).containsExactlyInAnyOrder("WELCOME", "TRANSFER_SENT")
        assertThat(kindsOf(erin)).containsExactlyInAnyOrder("WELCOME", "TRANSFER_RECEIVED")
        awaitEmpty(TRANSFERS_QUEUE)
        assertThat(visibleMessages(TRANSFERS_DLQ)).isEqualTo(transfersDlqAtStart)
    }

    /** Receives (and leaves visible) the DLQ message of [group], returning how many times it had been received. */
    private fun receiveCountInDlq(
        dlq: String,
        group: String,
    ): Int =
        sqs
            .receiveMessage {
                it
                    .queueUrl(queueUrl(dlq))
                    .maxNumberOfMessages(10)
                    .visibilityTimeout(0)
                    .messageSystemAttributeNames(
                        MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT,
                        MessageSystemAttributeName.MESSAGE_GROUP_ID,
                    )
            }.messages()
            .single { it.attributes()[MessageSystemAttributeName.MESSAGE_GROUP_ID] == group }
            .attributes()
            .getValue(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)
            .toInt()

    private fun dlqGauge(queue: String): Double =
        meterRegistry
            .get("notification.dlq.messages")
            .tag("queue", queue)
            .gauge()
            .value()

    private fun kindsOf(customerId: UUID): List<String> =
        jdbc
            .sql("select kind from notifications where customer_id = ? order by kind")
            .param(customerId)
            .query(String::class.java)
            .list()
            .filterNotNull()

    private fun notificationsOfEvent(eventId: UUID): Int =
        jdbc
            .sql("select count(*) from notifications where source_event_id = ?")
            .param(eventId)
            .query(Int::class.java)
            .single()

    private fun processedRows(eventId: UUID): Int =
        jdbc
            .sql("select count(*) from processed_events where event_id = ?")
            .param(eventId)
            .query(Int::class.java)
            .single()

    private fun sendAccountEvent(
        accountId: UUID,
        body: String,
    ) = send(ACCOUNTS_QUEUE, accountId.toString(), body)

    private fun sendTransferEvent(
        transferEventId: UUID,
        body: String,
    ) = send(TRANSFERS_QUEUE, transferEventId.toString(), body)

    private fun send(
        queue: String,
        group: String,
        body: String,
    ) {
        sqs.sendMessage {
            it
                .queueUrl(queueUrl(queue))
                .messageGroupId(group)
                .messageDeduplicationId(UUID.randomUUID().toString())
                .messageBody(body)
                .messageAttributes(
                    mapOf(
                        "correlationId" to
                            MessageAttributeValue
                                .builder()
                                .dataType("String")
                                .stringValue(CORRELATION_ID)
                                .build(),
                    ),
                )
        }
    }

    private fun accountOpened(
        accountId: UUID,
        customerId: UUID,
    ) = envelope(
        UUID.randomUUID(),
        "AccountOpened",
        """{"accountId":"$accountId","customerId":"$customerId","type":"STANDARD"}""",
    )

    private fun transferCompleted(
        eventId: UUID,
        source: UUID,
        destination: UUID,
    ) = envelope(
        eventId,
        "TransferCompleted",
        """{"transferId":"${UUID.randomUUID()}","sourceAccountId":"$source","destinationAccountId":"$destination",""" +
            """"amount":2500,"description":null}""",
    )

    private fun envelope(
        eventId: UUID,
        eventType: String,
        payload: String,
    ) = """{"eventId":"$eventId","eventType":"$eventType","eventVersion":1,"occurredAt":"${Instant.now()}",""" +
        """"aggregateId":"agg","correlationId":"$ENVELOPE_CORRELATION_ID","payload":$payload}"""

    private fun visibleMessages(queue: String): Int = attribute(queue, QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)

    private fun notVisibleMessages(queue: String): Int = attribute(queue, QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE)

    private fun attribute(
        queue: String,
        name: QueueAttributeName,
    ): Int =
        sqs
            .getQueueAttributes { it.queueUrl(queueUrl(queue)).attributeNames(name) }
            .attributes()
            .getValue(name)
            .toInt()

    private fun awaitEmpty(queue: String) = await { visibleMessages(queue) == 0 && notVisibleMessages(queue) == 0 }

    private fun await(
        timeout: Duration = Duration.ofSeconds(30),
        condition: () -> Boolean,
    ) {
        val deadline = Instant.now().plus(timeout)
        while (!condition()) {
            check(Instant.now().isBefore(deadline)) { "condition not met within $timeout" }
            Thread.sleep(100)
        }
    }

    companion object {
        private const val ACCOUNTS_QUEUE = "a5-notification-accounts.fifo"
        private const val TRANSFERS_QUEUE = "a5-notification-transfers.fifo"
        private const val ACCOUNTS_DLQ = "a5-notification-accounts-dlq.fifo"
        private const val TRANSFERS_DLQ = "a5-notification-transfers-dlq.fifo"
        private const val MAX_RECEIVE_COUNT = 5
        private const val VISIBILITY_SECONDS = 1
        private const val CORRELATION_ID = "a5-correlation"
        private const val ENVELOPE_CORRELATION_ID = "envelope-correlation"

        private val localStack: LocalStackContainer by lazy {
            LocalStackTestSupport.container(listOf("sqs")).also { it.start() }
        }
        private val sqs: SqsClient by lazy { LocalStackTestSupport.sqsClient(localStack) }

        private fun queueUrl(queue: String): String = sqs.getQueueUrl { it.queueName(queue) }.queueUrl()

        private fun createQueue(
            name: String,
            attributes: Map<QueueAttributeName, String> = emptyMap(),
        ): String =
            sqs
                .createQueue {
                    it.queueName(name).attributes(
                        mapOf(QueueAttributeName.FIFO_QUEUE to "true") + attributes,
                    )
                }.queueUrl()

        private fun createWithDlq(
            queue: String,
            dlq: String,
        ) {
            val dlqArn =
                sqs
                    .getQueueAttributes {
                        it.queueUrl(createQueue(dlq)).attributeNames(QueueAttributeName.QUEUE_ARN)
                    }.attributes()
                    .getValue(QueueAttributeName.QUEUE_ARN)
            createQueue(
                queue,
                mapOf(
                    QueueAttributeName.VISIBILITY_TIMEOUT to VISIBILITY_SECONDS.toString(),
                    QueueAttributeName.REDRIVE_POLICY to
                        """{"deadLetterTargetArn":"$dlqArn","maxReceiveCount":"$MAX_RECEIVE_COUNT"}""",
                ),
            )
        }

        /** Queues exist before the context starts, since the listener containers resolve their URLs on startup. */
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            createWithDlq(ACCOUNTS_QUEUE, ACCOUNTS_DLQ)
            createWithDlq(TRANSFERS_QUEUE, TRANSFERS_DLQ)
            NotificationIntegrationTest.databaseProperties(registry)
            registry.add("notification.queues.accounts") { ACCOUNTS_QUEUE }
            registry.add("notification.queues.transfers") { TRANSFERS_QUEUE }
            registry.add("notification.queues.accounts-dlq") { ACCOUNTS_DLQ }
            registry.add("notification.queues.transfers-dlq") { TRANSFERS_DLQ }
            registry.add("spring.cloud.aws.sqs.endpoint") { localStack.endpoint.toString() }
            registry.add("spring.cloud.aws.region.static") { localStack.region }
            registry.add("spring.cloud.aws.credentials.access-key") { localStack.accessKey }
            registry.add("spring.cloud.aws.credentials.secret-key") { localStack.secretKey }
        }
    }
}
