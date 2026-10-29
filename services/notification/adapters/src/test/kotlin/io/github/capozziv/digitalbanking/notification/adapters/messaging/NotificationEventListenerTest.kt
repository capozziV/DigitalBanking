package io.github.capozziv.digitalbanking.notification.adapters.messaging

import io.github.capozziv.digitalbanking.notification.application.UnknownAccountException
import io.github.capozziv.digitalbanking.notification.application.port.IdGenerator
import io.github.capozziv.digitalbanking.notification.application.port.KnownAccountRepository
import io.github.capozziv.digitalbanking.notification.application.port.NotificationCursor
import io.github.capozziv.digitalbanking.notification.application.port.NotificationRepository
import io.github.capozziv.digitalbanking.notification.application.port.ProcessedEventRepository
import io.github.capozziv.digitalbanking.notification.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.notification.application.usecase.ConsumeEventUseCase
import io.github.capozziv.digitalbanking.notification.domain.Notification
import io.github.capozziv.digitalbanking.outbox.OutboxEventEnvelope.CORRELATION_ID_MDC_KEY
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import java.time.Clock
import java.time.Instant
import java.util.UUID

class NotificationEventListenerTest {
    private val processed = mutableSetOf<UUID>()
    private val accounts = mutableMapOf<UUID, UUID>()
    private val saved = mutableListOf<Notification>()
    private val correlationIdsSeen = mutableListOf<String?>()

    private val transactions =
        object : TransactionRunner {
            override fun <T> inTransaction(block: () -> T): T {
                correlationIdsSeen += MDC.get(CORRELATION_ID_MDC_KEY)
                return block()
            }
        }

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
            transactions,
            object : IdGenerator {
                override fun newId(): UUID = UUID.randomUUID()
            },
            Clock.systemUTC(),
        )

    private val listener = NotificationEventListener(EventEnvelopeReader(), useCase)

    @AfterEach
    fun clearMdc() = MDC.clear()

    @Test
    fun `the correlation ID attribute is in the MDC while the event is consumed and removed afterwards`() {
        listener.onAccountEvent(accountOpened(UUID.randomUUID()), "corr-123")

        assertThat(correlationIdsSeen).containsExactly("corr-123")
        assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull()
        assertThat(saved).hasSize(1)
    }

    @Test
    fun `a message without correlation ID is still consumed`() {
        listener.onTransferEvent(accountOpened(UUID.randomUUID()), null)

        assertThat(correlationIdsSeen).containsExactly(null)
        assertThat(saved).hasSize(1)
    }

    @Test
    fun `a malformed body throws so the message is not acknowledged, and the MDC is cleared`() {
        assertThatThrownBy { listener.onAccountEvent("{", "corr-1") }.isInstanceOf(MalformedEventException::class.java)

        assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull()
        assertThat(processed).isEmpty()
    }

    @Test
    fun `an event about an unknown account throws so the message comes back`() {
        val body =
            envelope(
                "TransferCompleted",
                """{"transferId":"${UUID.randomUUID()}","sourceAccountId":"${UUID.randomUUID()}",""" +
                    """"destinationAccountId":"${UUID.randomUUID()}","amount":100,"description":null}""",
            )

        assertThatThrownBy { listener.onTransferEvent(body, "corr-2") }.isInstanceOf(UnknownAccountException::class.java)
        assertThat(processed).isEmpty()
    }

    @Test
    fun `an unsupported event is acknowledged without notifications`() {
        listener.onAccountEvent(envelope("LedgerEntryRecorded", """{"entryId":"x"}"""), "corr-3")

        assertThat(processed).hasSize(1)
        assertThat(saved).isEmpty()
    }

    private fun accountOpened(eventId: UUID) =
        envelope(
            "AccountOpened",
            """{"accountId":"${UUID.randomUUID()}","customerId":"${UUID.randomUUID()}","type":"FREE"}""",
            eventId,
        )

    private fun envelope(
        eventType: String,
        payload: String,
        eventId: UUID = UUID.randomUUID(),
    ) = """{"eventId":"$eventId","eventType":"$eventType","eventVersion":1,"payload":$payload}"""
}
