package io.github.capozziv.digitalbanking.statement.adapters.messaging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.github.capozziv.digitalbanking.observability.CorrelationId
import io.github.capozziv.digitalbanking.statement.application.port.StatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.application.port.StatementRepository
import io.github.capozziv.digitalbanking.statement.application.usecase.RecordStatementEntryUseCase
import io.github.capozziv.digitalbanking.statement.application.usecase.RegisterAccountUseCase
import io.github.capozziv.digitalbanking.statement.domain.StatementCursor
import io.github.capozziv.digitalbanking.statement.domain.StatementEntry
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.UUID

class StatementEventListenerTest {
    private val accounts = mutableMapOf<UUID, UUID>()

    private val repository =
        object : StatementRepository {
            override fun insertEntry(entry: StatementEntry) = error("not used")

            override fun findEntry(
                accountId: UUID,
                sequence: Long,
            ) = error("not used")

            override fun insertAccount(
                accountId: UUID,
                customerId: UUID,
            ) = accounts.putIfAbsent(accountId, customerId) == null

            override fun accountExists(accountId: UUID) = accountId in accounts

            override fun findCustomerId(accountId: UUID) = accounts[accountId]

            override fun findPage(
                accountId: UUID,
                cursor: StatementCursor?,
                limit: Int,
            ): StatementPage = error("not used")
        }

    private val cache =
        object : StatementFirstPageCache {
            override fun get(accountId: UUID) = error("not used")

            override fun put(
                accountId: UUID,
                page: StatementPage,
            ) = error("not used")

            override fun evict(accountId: UUID) = error("not used")
        }

    private val listener =
        StatementEventListener(
            EventEnvelopeReader(),
            RecordStatementEntryUseCase(repository, cache),
            RegisterAccountUseCase(repository),
        )

    private val logger = LoggerFactory.getLogger(StatementEventListener::class.java) as Logger
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

    @Test
    fun `the failure WARN carries only the exception message, since the container logs the stack trace`() {
        assertThatThrownBy { listener.onAccountEvent("{") }.isInstanceOf(MalformedEventException::class.java)

        val warning = logged.list.single { it.level == Level.WARN }
        assertThat(warning.throwableProxy).isNull()
        assertThat(warning.formattedMessage).contains(MalformedEventException::class.java.simpleName)
    }

    @Test
    fun `a failure leaves the correlation ID in the MDC for the container's error line`() {
        MDC.put(CorrelationId.MDC_KEY, "corr-fail")

        assertThatThrownBy { listener.onAccountEvent("{") }.isInstanceOf(MalformedEventException::class.java)

        assertThat(logged.list.single { it.level == Level.WARN }.mdcPropertyMap[CorrelationId.MDC_KEY]).isEqualTo("corr-fail")
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isEqualTo("corr-fail")
    }

    @Test
    fun `account opened is registered and logged`() {
        val accountId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val body =
            """{"eventId":"${UUID.randomUUID()}","eventType":"AccountOpened","eventVersion":1,""" +
                """"occurredAt":"2026-11-16T10:00:00Z","aggregateId":"$accountId","correlationId":"c",""" +
                """"payload":{"accountId":"$accountId","customerId":"$customerId","type":"STANDARD"}}"""

        listener.onAccountEvent(body)

        assertThat(accounts).containsEntry(accountId, customerId)
        assertThat(logged.list.map { it.formattedMessage }).contains("account $accountId registered")
    }
}
