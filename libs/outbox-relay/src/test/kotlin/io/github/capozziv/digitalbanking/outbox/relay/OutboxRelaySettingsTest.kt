package io.github.capozziv.digitalbanking.outbox.relay

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration

class OutboxRelaySettingsTest {
    @Test
    fun `defaults bound the batch, the alert, each publish and the whole cycle`() {
        val settings = OutboxRelaySettings(lockKey = 1)

        assertThat(settings.batchSize).isEqualTo(100)
        assertThat(settings.alertAfterAttempts).isEqualTo(10)
        assertThat(settings.publishTimeout).isEqualTo(Duration.ofSeconds(2))
        assertThat(settings.maxCycleDuration).isEqualTo(Duration.ofSeconds(10))
    }

    @Test
    fun `durations must be positive`() {
        assertThatThrownBy { OutboxRelaySettings(lockKey = 1, publishTimeout = Duration.ZERO) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { OutboxRelaySettings(lockKey = 1, maxCycleDuration = Duration.ofSeconds(-1)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
