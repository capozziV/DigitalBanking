package io.github.capozziv.digitalbanking.notification.adapters.messaging

import io.github.capozziv.digitalbanking.notification.application.port.IdGenerator
import io.github.capozziv.digitalbanking.notification.application.port.KnownAccountRepository
import io.github.capozziv.digitalbanking.notification.application.port.NotificationCursor
import io.github.capozziv.digitalbanking.notification.application.port.NotificationRepository
import io.github.capozziv.digitalbanking.notification.application.port.ProcessedEventRepository
import io.github.capozziv.digitalbanking.notification.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.notification.application.usecase.ConsumeEventUseCase
import io.github.capozziv.digitalbanking.notification.domain.Notification
import io.github.capozziv.digitalbanking.notification.domain.NotificationKind
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import sh.stubborn.contract.stubrunner.StubsMode
import sh.stubborn.contract.stubrunner.junit.StubRunnerExtension
import sh.stubborn.contract.verifier.converter.YamlContract
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Consumer side of the message contracts of accounts and transfers: the Stub Runner triggers each contract, and its
 * message (the outbox envelope, as the relay publishes it) goes straight to the handler of NotificationEventListener
 * for the topic it was sent to, with no SQS.
 */
class EventContractsTest {
    private val processed = mutableListOf<UUID>()
    private val accounts = mutableMapOf<UUID, UUID>()
    private val saved = mutableListOf<Notification>()

    private val useCase =
        ConsumeEventUseCase(
            object : ProcessedEventRepository {
                override fun markProcessed(
                    eventId: UUID,
                    eventType: String,
                    at: Instant,
                ) = processed.add(eventId)
            },
            object : KnownAccountRepository {
                override fun save(
                    accountId: UUID,
                    customerId: UUID,
                ) {
                    accounts.putIfAbsent(accountId, customerId)
                }

                override fun customerOf(accountId: UUID) = accounts[accountId]
            },
            object : NotificationRepository {
                override fun saveAll(notifications: List<Notification>) {
                    saved += notifications
                }

                override fun page(
                    customerId: UUID,
                    cursor: NotificationCursor?,
                    size: Int,
                ) = error("not used")

                override fun countUnread(customerId: UUID) = error("not used")

                override fun find(id: UUID) = error("not used")

                override fun markReadUpTo(
                    customerId: UUID,
                    upTo: Notification,
                    at: Instant,
                ) = error("not used")
            },
            object : TransactionRunner {
                override fun <T> inTransaction(block: () -> T): T = block()
            },
            object : IdGenerator {
                override fun newId(): UUID = UUID.randomUUID()
            },
            Clock.systemUTC(),
        )

    private val listener = NotificationEventListener(EventEnvelopeReader(), useCase)

    @BeforeEach
    fun routeMessagesToTheListener() {
        listenerSender.listener = listener
    }

    @AfterEach
    fun stopRouting() {
        listenerSender.listener = null
    }

    @Test
    fun `account opened welcomes the owner`() {
        assertThat(stubs.trigger("accountOpened")).isTrue()

        assertThat(processed).containsExactly(UUID.fromString("0198a1b2-0000-7000-8000-0000000e0001"))
        assertThat(saved).singleElement().satisfies({
            assertThat(it.customerId).isEqualTo(SENDER)
            assertThat(it.kind).isEqualTo(NotificationKind.WELCOME)
            assertThat(it.data).isEqualTo(mapOf("accountId" to SOURCE_ACCOUNT.toString(), "accountType" to "FREE"))
        })
    }

    @Test
    fun `an account type change tells the owner the new type and its overdraft limit`() {
        accounts[SOURCE_ACCOUNT] = SENDER

        assertThat(stubs.trigger("accountTypeChanged")).isTrue()

        assertThat(saved).singleElement().satisfies({
            assertThat(it.customerId).isEqualTo(SENDER)
            assertThat(it.kind).isEqualTo(NotificationKind.ACCOUNT_TYPE_CHANGED)
            assertThat(it.data).isEqualTo(
                mapOf("accountId" to SOURCE_ACCOUNT.toString(), "accountType" to "GOLD", "overdraftLimitCents" to 200000L),
            )
        })
    }

    @Test
    fun `a blocked account tells the owner`() {
        assertStatusNotification("accountBlocked", "0198a1b2-0000-7000-8000-0000000e0005", NotificationKind.ACCOUNT_BLOCKED)
    }

    @Test
    fun `an unblocked account tells the owner`() {
        assertStatusNotification("accountUnblocked", "0198a1b2-0000-7000-8000-0000000e0006", NotificationKind.ACCOUNT_UNBLOCKED)
    }

    @Test
    fun `a closed account tells the owner`() {
        assertStatusNotification("accountClosed", "0198a1b2-0000-7000-8000-0000000e0007", NotificationKind.ACCOUNT_CLOSED)
    }

    private fun assertStatusNotification(
        label: String,
        eventId: String,
        kind: NotificationKind,
    ) {
        assertThat(stubs.trigger(label)).isTrue()

        assertThat(processed).containsExactly(UUID.fromString(eventId))
        assertThat(saved).singleElement().satisfies({
            assertThat(it.customerId).isEqualTo(SENDER)
            assertThat(it.kind).isEqualTo(kind)
            assertThat(it.data).isEqualTo(mapOf("accountId" to SOURCE_ACCOUNT.toString()))
        })
    }

    @Test
    fun `a ledger entry is acknowledged without notifications`() {
        assertThat(stubs.trigger("creditRecorded")).isTrue()

        assertThat(processed).containsExactly(UUID.fromString("0198a1b2-0000-7000-8000-0000000e0002"))
        assertThat(saved).isEmpty()
    }

    @Test
    fun `a completed transfer notifies the sender and the receiver`() {
        knowBothAccounts()

        assertThat(stubs.trigger("transferCompleted")).isTrue()

        assertThat(saved)
            .extracting({ it.customerId }, { it.kind }, { it.data })
            .containsExactly(
                tuple(SENDER, NotificationKind.TRANSFER_SENT, transferData()),
                tuple(RECEIVER, NotificationKind.TRANSFER_RECEIVED, transferData()),
            )
    }

    @Test
    fun `a failed transfer tells the sender why`() {
        knowBothAccounts()

        assertThat(stubs.trigger("transferFailed")).isTrue()

        assertThat(saved)
            .extracting({ it.customerId }, { it.kind }, { it.data })
            .containsExactly(
                tuple(SENDER, NotificationKind.TRANSFER_FAILED, transferData() + ("reason" to "insufficient-available-balance")),
            )
    }

    @Test
    fun `a compensated transfer tells the sender the amount came back`() {
        knowBothAccounts()

        assertThat(stubs.trigger("transferCompensated")).isTrue()

        assertThat(saved)
            .extracting({ it.customerId }, { it.kind }, { it.data })
            .containsExactly(tuple(SENDER, NotificationKind.TRANSFER_COMPENSATED, transferData()))
    }

    private fun knowBothAccounts() {
        accounts[SOURCE_ACCOUNT] = SENDER
        accounts[DESTINATION_ACCOUNT] = RECEIVER
    }

    private fun transferData(): Map<String, Any> =
        mapOf(
            "transferId" to "0198a1b2-0000-7000-8000-00000000f001",
            "sourceAccountId" to SOURCE_ACCOUNT.toString(),
            "destinationAccountId" to DESTINATION_ACCOUNT.toString(),
            "amountCents" to 5000L,
        )

    /** Delivers each message to the handler of the queue subscribed to the topic it was sent to. */
    class ListenerSender : MessageVerifierSender<Any> {
        @Volatile
        var listener: NotificationEventListener? = null

        override fun send(
            message: Any,
            destination: String,
            contract: YamlContract?,
        ) = deliver(message, destination)

        override fun <T : Any?> send(
            payload: T,
            headers: Map<String, Any>?,
            destination: String,
            contract: YamlContract?,
        ) = deliver(payload, destination)

        private fun deliver(
            payload: Any?,
            destination: String,
        ) {
            val target = checkNotNull(listener) { "no listener to deliver to" }
            when (destination) {
                ACCOUNTS_TOPIC -> target.onAccountEvent(payload as String)
                TRANSFERS_TOPIC -> target.onTransferEvent(payload as String)
                else -> error("notification does not subscribe to $destination")
            }
        }
    }

    companion object {
        private const val ACCOUNTS_TOPIC = "accounts-events.fifo"
        private const val TRANSFERS_TOPIC = "transfers-events.fifo"
        private val SOURCE_ACCOUNT: UUID = UUID.fromString("0198a1b2-0000-7000-8000-000000000001")
        private val DESTINATION_ACCOUNT: UUID = UUID.fromString("0198a1b2-0000-7000-8000-000000000002")
        private val SENDER: UUID = UUID.fromString("0198a1b2-0000-7000-8000-0000000000c1")
        private val RECEIVER: UUID = UUID.fromString("0198a1b2-0000-7000-8000-0000000000c2")

        private val listenerSender = ListenerSender()

        @JvmField
        @RegisterExtension
        val stubs: StubRunnerExtension =
            StubRunnerExtension()
                .stubsMode(StubsMode.CLASSPATH)
                .messageVerifierSender(listenerSender)
                .downloadStubs(
                    "io.github.capozziv.digitalbanking.accounts:bootstrap",
                    "io.github.capozziv.digitalbanking.transfers:bootstrap",
                )
    }
}
