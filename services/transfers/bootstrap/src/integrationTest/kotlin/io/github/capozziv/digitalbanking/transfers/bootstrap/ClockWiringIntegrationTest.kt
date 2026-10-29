package io.github.capozziv.digitalbanking.transfers.bootstrap

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Clock

/** The service has a single Clock bean, ticking in microseconds (the precision of timestamptz). */
class ClockWiringIntegrationTest : TransfersIntegrationTest() {
    @Autowired
    private lateinit var clock: Clock

    @Test
    fun `the clock bean ticks in whole microseconds`() {
        val instants = List(10_000) { clock.instant() }

        assertThat(instants).allSatisfy { assertThat(it.nano % 1_000).isZero() }
    }
}
