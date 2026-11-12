package io.github.capozziv.digitalbanking.statement.bootstrap.messaging

import io.github.capozziv.digitalbanking.statement.application.port.StatementRepository
import io.github.capozziv.digitalbanking.statement.bootstrap.StatementIntegrationTest
import io.github.capozziv.digitalbanking.statement.domain.EntryKind
import io.micrometer.core.instrument.MeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.test.context.TestPropertySource
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Consumption of the accounts FIFO queue against LocalStack, with a redrive policy of 5 receives and a visibility
 * timeout of 1 s (see [StatementIntegrationTest]): redeliveries are no-ops, a bad message ends in the DLQ without
 * holding other accounts. The queue and the table are shared, so each test uses its own account ids and compares
 * the DLQ with its depth when it started.
 */
@ExtendWith(OutputCaptureExtension::class)
@TestPropertySource(properties = ["statement.dlq-metrics.refresh-interval=1s"])
class StatementConsumptionIntegrationTest : StatementIntegrationTest() {
    @Autowired
    private lateinit var repository: StatementRepository

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    private var dlqAtStart = 0

    @BeforeEach
    fun recordDlqDepth() {
        dlqAtStart = visibleMessages(ACCOUNTS_DLQ)
    }

    @Test
    fun `the same message delivered 20 times records one entry`(output: CapturedOutput) {
        val accountId = UUID.randomUUID()
        val body = ledgerEntryRecorded(accountId, sequence = 1, entryId = UUID.randomUUID())

        // Each send has its own deduplication ID, so SQS delivers all 20: the redelivery after a lost ack.
        repeat(20) { send(accountId, body) }

        // SQS is at-least-once, so one of the 20 messages may arrive twice: only a lower bound is stable.
        await { output.out.lines().count { it.contains("entry $accountId/1 ") } >= 20 }
        awaitEmpty()
        assertThat(output.out.lines().count { it.contains("entry $accountId/1 recorded") }).isEqualTo(1)
        assertThat(output.out.lines().count { it.contains("entry $accountId/1 already recorded") }).isGreaterThanOrEqualTo(19)
        assertThat(repository.findPage(accountId, null, 10).entries).hasSize(1)
        assertThat(visibleMessages(ACCOUNTS_DLQ)).isEqualTo(dlqAtStart)
    }

    @Test
    fun `entries delivered out of order are listed in sequence order`() {
        val accountId = UUID.randomUUID()

        listOf(3L, 1L, 2L).forEach { send(accountId, ledgerEntryRecorded(accountId, it, UUID.randomUUID())) }

        await { repository.findPage(accountId, null, 10).entries.size == 3 }
        val entries = repository.findPage(accountId, null, 10).entries
        assertThat(entries.map { it.sequence }).containsExactly(3L, 2L, 1L)
        assertThat(entries.map { it.kind }).containsOnly(EntryKind.CREDIT)
    }

    @Test
    fun `account opened registers the account once`() {
        val accountId = UUID.randomUUID()
        val body = envelope("AccountOpened", 1, """{"accountId":"$accountId","customerId":"${UUID.randomUUID()}","type":"FREE"}""")

        repeat(3) { send(accountId, body) }

        await { repository.accountExists(accountId) }
        awaitEmpty()
        assertThat(visibleMessages(ACCOUNTS_DLQ)).isEqualTo(dlqAtStart)
    }

    @Test
    fun `a malformed message ends in the DLQ and other accounts keep flowing`() {
        val brokenAccount = UUID.randomUUID()
        send(brokenAccount, """{"eventId":"${UUID.randomUUID()}","eventType":"LedgerEntryRecorded","eventVersion":1,"payload":{}}""")
        val healthyAccount = UUID.randomUUID()
        send(healthyAccount, ledgerEntryRecorded(healthyAccount, 1, UUID.randomUUID()))

        // The other group is processed while the broken message is still being retried.
        await { repository.findPage(healthyAccount, null, 10).entries.size == 1 }
        assertThat(visibleMessages(ACCOUNTS_DLQ)).isEqualTo(dlqAtStart)

        await(Duration.ofSeconds(60)) { visibleMessages(ACCOUNTS_DLQ) == dlqAtStart + 1 }
        awaitEmpty()
        await { dlqGauge() == (dlqAtStart + 1).toDouble() }
    }

    @Test
    fun `AccountTypeChanged is acknowledged without effect`(output: CapturedOutput) {
        val accountId = UUID.randomUUID()

        send(accountId, envelope("AccountTypeChanged", 1, """{"accountId":"$accountId","newType":"GOLD"}"""))

        await { output.out.contains("event AccountTypeChanged v1 ignored") }
        awaitEmpty()
        assertThat(repository.accountExists(accountId)).isFalse()
        assertThat(visibleMessages(ACCOUNTS_DLQ)).isEqualTo(dlqAtStart)
    }

    @Test
    fun `a conflicting entry ends in the DLQ`(output: CapturedOutput) {
        val accountId = UUID.randomUUID()
        val storedEntryId = UUID.randomUUID()
        send(accountId, ledgerEntryRecorded(accountId, 1, storedEntryId))
        await { repository.findEntry(accountId, 1) != null }

        send(accountId, ledgerEntryRecorded(accountId, 1, UUID.randomUUID()))

        await(Duration.ofSeconds(60)) { visibleMessages(ACCOUNTS_DLQ) == dlqAtStart + 1 }
        awaitEmpty()
        assertThat(output.out.lines().any { it.contains("ERROR") && it.contains("conflicting statement entry") }).isTrue()
        assertThat(repository.findEntry(accountId, 1)!!.entryId).isEqualTo(storedEntryId)
    }

    @Test
    fun `the correlation id of the SQS message attribute reaches the log of the consumption`(output: CapturedOutput) {
        val accountId = UUID.randomUUID()

        send(accountId, ledgerEntryRecorded(accountId, 1, UUID.randomUUID()))

        await { output.out.lines().any { it.contains("entry $accountId/1 recorded") } }
        val line = output.out.lines().first { it.contains("entry $accountId/1 recorded") }
        // The envelope carries another id, so only the message attribute can put this one in the log.
        assertThat(line).contains("[$CORRELATION_ID]").doesNotContain(ENVELOPE_CORRELATION_ID)
    }

    private fun dlqGauge(): Double =
        meterRegistry
            .get("statement.dlq.messages")
            .tag("queue", ACCOUNTS_DLQ)
            .gauge()
            .value()

    private fun send(
        accountId: UUID,
        body: String,
    ) {
        sqs.sendMessage {
            it
                .queueUrl(queueUrl(ACCOUNTS_QUEUE))
                .messageGroupId(accountId.toString())
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

    private fun ledgerEntryRecorded(
        accountId: UUID,
        sequence: Long,
        entryId: UUID,
    ) = envelope(
        "LedgerEntryRecorded",
        1,
        """{"entryId":"$entryId","accountId":"$accountId","sequence":$sequence,"kind":"CREDIT","amount":1000,""" +
            """"balanceAfter":${1000 * sequence},"description":"Deposit","idempotencyKey":"key-$entryId",""" +
            """"createdAt":"${Instant.now()}"}""",
    )

    private fun envelope(
        eventType: String,
        version: Int,
        payload: String,
    ) = """{"eventId":"${UUID.randomUUID()}","eventType":"$eventType","eventVersion":$version,"occurredAt":"${Instant.now()}",""" +
        """"aggregateId":"agg","correlationId":"$ENVELOPE_CORRELATION_ID","payload":$payload}"""

    private fun queueUrl(queue: String): String = sqs.getQueueUrl { it.queueName(queue) }.queueUrl()

    private fun visibleMessages(queue: String): Int = attribute(queue, QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)

    private fun attribute(
        queue: String,
        name: QueueAttributeName,
    ): Int =
        sqs
            .getQueueAttributes { it.queueUrl(queueUrl(queue)).attributeNames(name) }
            .attributes()
            .getValue(name)
            .toInt()

    private fun awaitEmpty() =
        await {
            visibleMessages(ACCOUNTS_QUEUE) == 0 &&
                attribute(ACCOUNTS_QUEUE, QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE) == 0
        }

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

    private companion object {
        const val CORRELATION_ID = "task5-correlation"
        const val ENVELOPE_CORRELATION_ID = "envelope-correlation"
    }
}
