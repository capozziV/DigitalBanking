package io.github.capozziv.digitalbanking.transfers.bootstrap

import io.github.capozziv.digitalbanking.outbox.JdbcOutboxWriter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils
import java.time.Clock

/**
 * The service has a single Clock bean, ticking in microseconds (the precision of timestamptz), and the outbox writer
 * uses that bean instead of a clock of its own.
 */
class ClockWiringIntegrationTest : TransfersIntegrationTest() {
    @Autowired
    private lateinit var clock: Clock

    @Autowired
    private lateinit var outboxWriter: JdbcOutboxWriter

    @Test
    fun `the clock bean ticks in whole microseconds`() {
        val instants = List(10_000) { clock.instant() }

        assertThat(instants).allSatisfy { assertThat(it.nano % 1_000).isZero() }
    }

    @Test
    fun `the outbox writer uses the clock bean`() {
        assertThat(ReflectionTestUtils.getField(outboxWriter, "clock")).isSameAs(clock)
    }
}
