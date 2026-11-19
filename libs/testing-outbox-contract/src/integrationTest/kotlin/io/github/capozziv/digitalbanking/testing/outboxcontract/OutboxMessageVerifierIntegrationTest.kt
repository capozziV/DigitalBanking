package io.github.capozziv.digitalbanking.testing.outboxcontract

import io.github.capozziv.digitalbanking.outbox.JdbcOutboxWriter
import io.github.capozziv.digitalbanking.outbox.OutboxEventEnvelope
import io.github.capozziv.digitalbanking.outbox.OutboxMessage
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.jdbc.core.simple.JdbcClient
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class OutboxMessageVerifierIntegrationTest {
    private val now = Instant.parse("2026-11-18T12:00:00Z")
    private val jdbcClient = JdbcClient.create(OutboxContractDatabase.dataSource)
    private val writer = JdbcOutboxWriter(jdbcClient, Clock.fixed(now, ZoneOffset.UTC))
    private val verifier = OutboxMessageVerifier(jdbcClient, TOPIC_ARN)
    private val jsonMapper = JsonMapper.builder().build()

    @BeforeEach
    fun clearOutbox() {
        verifier.clear()
    }

    @AfterEach
    fun clearMdc() {
        MDC.clear()
    }

    @Test
    fun `receives the recorded event as the envelope the relay publishes, with the correlation id of the MDC`() {
        MDC.put(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY, "corr-1")
        writer.write(OutboxMessage("AccountOpened", 1, "acc-1", """{"accountId":"acc-1","amount":3700,"description":null}"""))

        val message = verifier.receive("accounts-events.fifo", null)

        val envelope = jsonMapper.readTree(message.payload as String)
        assertThat(envelope["eventId"].asString()).matches("[0-9a-f-]{36}")
        assertThat(envelope["eventType"].asString()).isEqualTo("AccountOpened")
        assertThat(envelope["eventVersion"].asInt()).isEqualTo(1)
        assertThat(envelope["occurredAt"].asString()).isEqualTo("2026-11-18T12:00:00Z")
        assertThat(envelope["aggregateId"].asString()).isEqualTo("acc-1")
        assertThat(envelope["correlationId"].asString()).isEqualTo("corr-1")
        assertThat(envelope["payload"]).isEqualTo(jsonMapper.readTree("""{"accountId":"acc-1","amount":3700,"description":null}"""))
    }

    @Test
    fun `a received event is marked published, so it is not received again`() {
        writer.write(OutboxMessage("AccountOpened", 1, "acc-1", """{"accountId":"acc-1"}"""))
        verifier.receive("accounts-events.fifo", null)

        assertThatThrownBy { verifier.receive("accounts-events.fifo", null) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("no event")
    }

    @Test
    fun `a destination other than the topic of the service fails`() {
        writer.write(OutboxMessage("AccountOpened", 1, "acc-1", """{"accountId":"acc-1"}"""))

        assertThatThrownBy { verifier.receive("transfers-events.fifo", null) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("accounts-events.fifo")
    }

    @Test
    fun `more than one pending event fails, since a trigger must record exactly one`() {
        writer.write(OutboxMessage("AccountOpened", 1, "acc-1", """{"accountId":"acc-1"}"""))
        writer.write(OutboxMessage("AccountOpened", 1, "acc-2", """{"accountId":"acc-2"}"""))

        assertThatThrownBy { verifier.receive("accounts-events.fifo", null) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("2 events")
    }

    @Test
    fun `sending is not supported, since the producer only publishes`() {
        assertThatThrownBy { verifier.send("{}", emptyMap<String, Any>(), "accounts-events.fifo", null) }
            .isInstanceOf(UnsupportedOperationException::class.java)
    }

    private companion object {
        const val TOPIC_ARN = "arn:aws:sns:us-east-1:000000000000:accounts-events.fifo"
    }
}
