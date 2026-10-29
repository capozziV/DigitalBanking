package io.github.capozziv.digitalbanking.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.RegisterExtension
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Done criteria 1 to 6 of stage 3, with `accounts`, `transfers` and `notification` running as real processes, the
 * notification queues and DLQs created by the LocalStack init script of the local environment (visibility 2 s,
 * maxReceiveCount 5) and the events reaching them through the SNS topics, as in the local environment.
 *
 * Every transfer is made between accounts whose welcome notification has already arrived: the queues of the two
 * producers have no order between them, and an event about an account `notification` does not know yet goes back to
 * the queue (and, with the short visibility of the tests, could reach the DLQ). Every wait is a condition polled with a
 * time limit, never a blind sleep.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class NotificationEndToEndIntegrationTest {
    private val environment = EndToEndEnvironment()

    @RegisterExtension
    val processLogs = ProcessLogsOnFailureExtension { environment.processes() }

    private val accounts get() = environment.accountsApi
    private val transfers get() = environment.transfersApi
    private val notifications get() = environment.notificationApi
    private val queues get() = environment.notificationQueues

    @BeforeAll
    fun start() = environment.start()

    @AfterAll
    fun stop() = environment.close()

    @BeforeEach
    fun servicesRunning() {
        environment.proxy.releaseAll()
        environment.accounts.ensureRunning()
        environment.transfers.ensureRunning()
        environment.notification.ensureRunning()
    }

    @Test
    @Order(1)
    fun `opening an account notifies its customer with a welcome`() {
        val customer = UUID.randomUUID()

        val account = accounts.openAccount(customer)

        val welcome = notifications.awaitKinds(customer, listOf("WELCOME"), NOTIFICATION_TIMEOUT).single()
        assertThat(welcome.get("title").asString()).isEqualTo("Bem-vindo ao banco!")
        assertThat(welcome.get("body").asString()).contains("Free").contains(account.toString())
        assertThat(welcome.get("data").get("accountId").asString()).isEqualTo(account.toString())
    }

    @Test
    @Order(2)
    fun `changing the account type notifies the new type and its overdraft limit`() {
        val (customer, account) = welcomedAccount()

        accounts.changeType(account, "GOLD")

        val changed = notifications.awaitKinds(customer, listOf("WELCOME", "ACCOUNT_TYPE_CHANGED"), NOTIFICATION_TIMEOUT)
        val body = changed.single { it.get("kind").asString() == "ACCOUNT_TYPE_CHANGED" }.get("body").asString()
        assertThat(body).isEqualTo("Sua conta agora é Gold. Seu limite de cheque especial passou a ser R$ 2.000,00.")
    }

    @Test
    @Order(3)
    fun `a completed transfer notifies sender and receiver once, even when its event is delivered 20 more times`() {
        val (sender, source) = welcomedAccount()
        val (receiver, destination) = welcomedAccount()
        accounts.credit(source, 1_000)
        val key = UUID.randomUUID().toString()

        val dlqBefore = queues.transfersDlqDepth()
        val id = transfers.requestTransfer(source, destination, 300, key).transferId()

        assertThat(transfers.awaitTerminal(id, SAGA_FINISH_TIMEOUT)).isEqualTo("COMPLETED")
        val sent = notifications.awaitKinds(sender, listOf("WELCOME", "TRANSFER_SENT"), NOTIFICATION_TIMEOUT)
        val received = notifications.awaitKinds(receiver, listOf("WELCOME", "TRANSFER_RECEIVED"), NOTIFICATION_TIMEOUT)
        assertThat(sent.single { it.get("kind").asString() == "TRANSFER_SENT" }.get("body").asString())
            .isEqualTo("Você enviou R$ 3,00 para a conta $destination.")
        assertThat(received.single { it.get("kind").asString() == "TRANSFER_RECEIVED" }.get("body").asString())
            .isEqualTo("Você recebeu R$ 3,00 da conta $source.")

        // The published event, as the outbox relay sent it, delivered 20 more times with new deduplication ids.
        val event = environment.transferEvents.awaitMessage(id, "TransferCompleted", EVENTS_TIMEOUT)
        repeat(REDELIVERIES) { queues.sendToTransfersQueue(event, deduplicationId = UUID.randomUUID().toString()) }
        queues.awaitTransfersQueueDrained(DRAIN_TIMEOUT)
        // Each redelivery was consumed (and recognised), not dropped by SQS deduplication.
        val eventId = mapper.readTree(event.body).get("eventId").asString()
        environment.notification.awaitLogLine(NOTIFICATION_TIMEOUT, count = REDELIVERIES) { "$eventId already processed" in it }

        assertThat(notifications.all(sender).map { it.get("kind").asString() }).containsExactlyInAnyOrder("WELCOME", "TRANSFER_SENT")
        assertThat(notifications.all(receiver).map { it.get("kind").asString() })
            .containsExactlyInAnyOrder("WELCOME", "TRANSFER_RECEIVED")
        assertThat(queues.transfersDlqDepth()).isEqualTo(dlqBefore)
    }

    @Test
    @Order(4)
    fun `the correlation ID of a transfer request reaches the log of notification`() {
        val (sender, source) = welcomedAccount()
        val (_, destination) = welcomedAccount()
        accounts.credit(source, 1_000)
        val key = UUID.randomUUID().toString()

        val id = transfers.requestTransfer(source, destination, 100, key).transferId()

        assertThat(transfers.awaitTerminal(id, SAGA_FINISH_TIMEOUT)).isEqualTo("COMPLETED")
        notifications.awaitKinds(sender, listOf("WELCOME", "TRANSFER_SENT"), NOTIFICATION_TIMEOUT)
        val correlationId = TransfersApiClient.correlationIdFor(key)
        environment.notification.awaitLogLine(NOTIFICATION_TIMEOUT) { "[$correlationId]" in it && "TransferCompleted" in it }
    }

    @Test
    @Order(5)
    fun `a refused transfer notifies the sender that nothing left the account`() {
        val (sender, source) = welcomedAccount()
        val (receiver, destination) = welcomedAccount()

        val response = transfers.requestTransfer(source, destination, 500)

        assertThat(response.status).describedAs(response.body).isEqualTo(422)
        assertThat(transfers.awaitTerminal(response.transferId(), SAGA_FINISH_TIMEOUT)).isEqualTo("FAILED")
        val failed = notifications.awaitKinds(sender, listOf("WELCOME", "TRANSFER_FAILED"), NOTIFICATION_TIMEOUT)
        assertThat(failed.single { it.get("kind").asString() == "TRANSFER_FAILED" }.get("body").asString())
            .isEqualTo(
                "Sua transferência de R$ 5,00 para a conta $destination foi recusada: saldo disponível insuficiente. " +
                    "Nenhum valor saiu da sua conta.",
            )
        assertThat(notifications.all(receiver).map { it.get("kind").asString() }).containsExactly("WELCOME")
    }

    @Test
    @Order(6)
    fun `a compensated transfer notifies the sender that the money came back`() {
        val (sender, source) = welcomedAccount()
        val (receiver, destination) = welcomedAccount()
        accounts.credit(source, 1_000)

        // As in the saga tests: the credit is held, accounts stops, and comes back only after the deadline.
        val hold = environment.proxy.holdNext("POST", "/accounts/$destination/credits")
        val request = CompletableFuture.supplyAsync { transfers.requestTransfer(source, destination, 300) }
        val id = hold.awaitHeld(CALL_HELD_TIMEOUT).transferId()
        environment.accounts.stop()
        hold.release()
        assertThat(hold.awaitForwarded(CALL_HELD_TIMEOUT)).isEqualTo(502)
        assertThat(request.get().status).isEqualTo(202)
        environment.transfersDatabase.awaitStepOutcome(id, "RESOLVE_CREDIT", "TRANSIENT_FAILURE", SAGA_FINISH_TIMEOUT)
        environment.accounts.start()

        assertThat(transfers.awaitTerminal(id, SAGA_FINISH_TIMEOUT)).isEqualTo("COMPENSATED")
        val compensated = notifications.awaitKinds(sender, listOf("WELCOME", "TRANSFER_COMPENSATED"), NOTIFICATION_TIMEOUT)
        assertThat(compensated.single { it.get("kind").asString() == "TRANSFER_COMPENSATED" }.get("body").asString())
            .isEqualTo(
                "Sua transferência de R$ 3,00 para a conta $destination não pôde ser concluída e o valor voltou para a sua conta.",
            )
        assertThat(notifications.all(receiver).map { it.get("kind").asString() }).containsExactly("WELCOME")
    }

    @Test
    @Order(7)
    fun `a malformed message reaches the DLQ after 5 receives while a transfer of another account is notified`() {
        val (sender, source) = welcomedAccount()
        val (_, destination) = welcomedAccount()
        accounts.credit(source, 1_000)
        val dlqBefore = queues.transfersDlqDepth()

        val malformedGroup = "malformed-${UUID.randomUUID()}"
        queues.sendRawToTransfersQueue(MALFORMED_BODY, groupId = malformedGroup)
        val id = transfers.requestTransfer(source, destination, 200).transferId()

        assertThat(transfers.awaitTerminal(id, SAGA_FINISH_TIMEOUT)).isEqualTo("COMPLETED")
        notifications.awaitKinds(sender, listOf("WELCOME", "TRANSFER_SENT"), NOTIFICATION_TIMEOUT)
        // The other group went on while the malformed message was still failing: it is not in the DLQ yet.
        assertThat(queues.transfersDlqDepth()).isEqualTo(dlqBefore)
        val dead = queues.awaitDeadLetter(malformedGroup, DLQ_TIMEOUT)
        assertThat(dead.body).isEqualTo(MALFORMED_BODY)
        // SQS carries the receive count into the DLQ; the receive that reads it here adds one.
        assertThat(dead.receiveCount).isEqualTo(MAX_RECEIVE_COUNT + 1)
        assertThat(queues.transfersDlqDepth()).isEqualTo(dlqBefore)
    }

    @Test
    @Order(8)
    fun `with notification stopped during 3 transfers, it notifies all 6 once it is back, without duplicates`() {
        val pairs = List(3) { welcomedAccount() to welcomedAccount() }
        pairs.forEach { (sender, _) -> accounts.credit(sender.second, 1_000) }

        environment.notification.stop()
        val ids = pairs.map { (sender, receiver) -> transfers.requestTransfer(sender.second, receiver.second, 100).transferId() }
        ids.forEach { assertThat(transfers.awaitTerminal(it, SAGA_FINISH_TIMEOUT)).isEqualTo("COMPLETED") }
        queues.awaitTransfersQueueDepth(ids.size, EVENTS_TIMEOUT)
        environment.notification.start()

        pairs.forEach { (sender, receiver) ->
            notifications.awaitKinds(sender.first, listOf("WELCOME", "TRANSFER_SENT"), NOTIFICATION_TIMEOUT)
            notifications.awaitKinds(receiver.first, listOf("WELCOME", "TRANSFER_RECEIVED"), NOTIFICATION_TIMEOUT)
        }
        queues.awaitTransfersQueueDrained(DRAIN_TIMEOUT)
        pairs.forEach { (sender, receiver) ->
            assertThat(notifications.all(sender.first).map { it.get("kind").asString() })
                .containsExactlyInAnyOrder("WELCOME", "TRANSFER_SENT")
            assertThat(notifications.all(receiver.first).map { it.get("kind").asString() })
                .containsExactlyInAnyOrder("WELCOME", "TRANSFER_RECEIVED")
        }
    }

    /** A new customer and account whose welcome notification has arrived, so `notification` knows the account. */
    private fun welcomedAccount(): Pair<UUID, UUID> {
        val customer = UUID.randomUUID()
        val account = accounts.openAccount(customer)
        notifications.awaitKinds(customer, listOf("WELCOME"), NOTIFICATION_TIMEOUT)
        return customer to account
    }

    private companion object {
        val mapper: JsonMapper = JsonMapper.builder().build()
        const val REDELIVERIES = 20
        const val MAX_RECEIVE_COUNT = 5
        const val MALFORMED_BODY = "this is not an event envelope"
        val NOTIFICATION_TIMEOUT: Duration = Duration.ofSeconds(30)
        val SAGA_FINISH_TIMEOUT: Duration = Duration.ofSeconds(90)
        val CALL_HELD_TIMEOUT: Duration = Duration.ofSeconds(30)
        val EVENTS_TIMEOUT: Duration = Duration.ofSeconds(30)
        val DRAIN_TIMEOUT: Duration = Duration.ofSeconds(30)

        /** 5 receives 2 s apart plus the time to move the message. */
        val DLQ_TIMEOUT: Duration = Duration.ofSeconds(60)
    }
}
