package io.github.capozziv.digitalbanking.transfers.bootstrap

import io.github.capozziv.digitalbanking.outbox.JdbcOutboxWriter
import io.github.capozziv.digitalbanking.outbox.OutboxMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * The outbox writer stamps `occurred_at` with the service Clock bean, not with a clock of its own: with a fixed
 * Clock as the bean, the stored instant is the fixed one. (A separate Spring context, because of the Clock override.)
 */
@Import(OutboxWriterClockIntegrationTest.FixedClockConfiguration::class)
class OutboxWriterClockIntegrationTest : TransfersIntegrationTest() {
    @Autowired
    private lateinit var outboxWriter: JdbcOutboxWriter

    @Test
    fun `the outbox writer stamps occurred_at with the clock bean`() {
        val aggregateId = "clock-wiring-test"

        outboxWriter.write(OutboxMessage("ClockWiringChecked", 1, aggregateId, "{}"))

        val occurredAt =
            ownerJdbc
                .sql("select occurred_at from outbox where aggregate_id = :id")
                .param("id", aggregateId)
                .query(OffsetDateTime::class.java)
                .single()
        assertThat(occurredAt.toInstant()).isEqualTo(FIXED_INSTANT)
        ownerJdbc.sql("delete from outbox where aggregate_id = :id").param("id", aggregateId).update()
    }

    @TestConfiguration(proxyBeanMethods = false)
    class FixedClockConfiguration {
        @Bean
        @Primary
        fun fixedClock(): Clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)
    }

    private companion object {
        val FIXED_INSTANT: Instant = Instant.parse("2026-01-02T03:04:05.123456Z")
    }
}
