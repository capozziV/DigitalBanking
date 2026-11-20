package io.github.capozziv.digitalbanking.notification.adapters.messaging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.github.capozziv.digitalbanking.notification.application.UnknownAccountException
import io.github.capozziv.digitalbanking.notification.application.port.CustomerNameRepository
import io.github.capozziv.digitalbanking.notification.application.port.IdGenerator
import io.github.capozziv.digitalbanking.notification.application.port.KnownAccountRepository
import io.github.capozziv.digitalbanking.notification.application.port.NotificationCursor
import io.github.capozziv.digitalbanking.notification.application.port.NotificationRepository
import io.github.capozziv.digitalbanking.notification.application.port.ProcessedEventRepository
import io.github.capozziv.digitalbanking.notification.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.notification.application.usecase.ConsumeEventUseCase
import io.github.capozziv.digitalbanking.notification.domain.Notification
import io.github.capozziv.digitalbanking.observability.CorrelationId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Clock
import java.time.Instant
import java.util.UUID

class NotificationEventListenerTest {
    private val processed = mutableSetOf<UUID>()
    private val accounts = mutableMapOf<UUID, UUID>()
    private val saved = mutableListOf<Notification>()
    private val names = mutableMapOf<UUID, String>()

    private val transactions =
        object : TransactionRunner {
            override fun <T> inTransaction(block: () -> T): T = block()
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
            object : CustomerNameRepository {
                override fun save(
                    customerId: UUID,
                    name: String,
                ) {
                    names[customerId] = name
                }

                override fun namesOfAccountOwners(accountIds: Set<UUID>) = error("not used")
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

    private val logger = LoggerFactory.getLogger(NotificationEventListener::class.java) as Logger
    private val logged = ListAppender<ILoggingEvent>()

    @BeforeEach
    fun captureLogs() {
        logger.level = Level.INFO
        logged.start()
        logger.addAppender(logged)
    }

    @AfterEach
    fun clearMdc() {
        logger.detachAppender(logged)
        MDC.clear()
    }

    private fun infoLines() = logged.list.filter { it.level == Level.INFO }.map { it.formattedMessage }

    @Test
    fun `a processed event, a duplicate and an ignored one are logged at INFO with type and event ID`() {
        val processedId = UUID.randomUUID()
        val ignoredId = UUID.randomUUID()
        val body = accountOpened(processedId)

        listener.onAccountEvent(body)
        listener.onAccountEvent(body)
        listener.onAccountEvent(envelope("LedgerEntryRecorded", "{}", ignoredId))

        assertThat(infoLines())
            .anyMatch { "AccountOpened" in it && "$processedId" in it && it.endsWith("processed") }
            .anyMatch { "AccountOpened" in it && "$processedId" in it && "already processed" in it }
            .anyMatch { "LedgerEntryRecorded" in it && "$ignoredId" in it && "ignored" in it }
    }

    @Test
    fun `a failure leaves the correlation ID in the MDC for the container's error line`() {
        MDC.put(CorrelationId.MDC_KEY, "corr-fail")

        assertThatThrownBy { listener.onAccountEvent("{") }.isInstanceOf(MalformedEventException::class.java)

        assertThat(logged.list.single { it.level == Level.WARN }.mdcPropertyMap[CorrelationId.MDC_KEY]).isEqualTo("corr-fail")
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isEqualTo("corr-fail")
    }

    @Test
    fun `the failure WARN carries only the exception message, since the container logs the stack trace`() {
        assertThatThrownBy { listener.onAccountEvent("{") }.isInstanceOf(MalformedEventException::class.java)

        val warning = logged.list.single { it.level == Level.WARN }
        assertThat(warning.throwableProxy).isNull()
        assertThat(warning.formattedMessage).contains(MalformedEventException::class.java.simpleName)
    }

    @Test
    fun `a malformed body throws so the message is not acknowledged`() {
        assertThatThrownBy { listener.onAccountEvent("{") }.isInstanceOf(MalformedEventException::class.java)

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

        assertThatThrownBy { listener.onTransferEvent(body) }.isInstanceOf(UnknownAccountException::class.java)
        assertThat(processed).isEmpty()
    }

    @Test
    fun `an unsupported event is acknowledged without notifications`() {
        listener.onAccountEvent(envelope("LedgerEntryRecorded", """{"entryId":"x"}"""))

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
