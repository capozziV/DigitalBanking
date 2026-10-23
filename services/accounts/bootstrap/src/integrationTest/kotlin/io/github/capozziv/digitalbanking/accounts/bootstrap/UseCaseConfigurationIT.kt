package io.github.capozziv.digitalbanking.accounts.bootstrap

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Clock

class UseCaseConfigurationIT : AccountsIntegrationTest() {
    @Autowired
    private lateinit var clock: Clock

    @Test
    fun `the application clock ticks in microseconds, the precision of timestamptz`() {
        val instants = (1..1_000).map { clock.instant() }

        assertThat(instants).allSatisfy { assertThat(it.nano % 1_000).isZero() }
    }
}
