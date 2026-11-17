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
    fun `retry backoff defaults to one second doubling up to thirty`() {
        val settings = OutboxRelaySettings(lockKey = 1)

        assertThat(settings.retryBackoff).isEqualTo(Duration.ofSeconds(1))
        assertThat(settings.maxRetryBackoff).isEqualTo(Duration.ofSeconds(30))
    }

    @Test
    fun `the maximum backoff cannot be below the initial one`() {
        assertThatThrownBy {
            OutboxRelaySettings(lockKey = 1, retryBackoff = Duration.ofSeconds(5), maxRetryBackoff = Duration.ofSeconds(1))
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `durations must be positive`() {
        assertThatThrownBy { OutboxRelaySettings(lockKey = 1, publishTimeout = Duration.ZERO) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { OutboxRelaySettings(lockKey = 1, maxCycleDuration = Duration.ofSeconds(-1)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `a discard waits thirty seconds for the relay lock by default`() {
        assertThat(OutboxRelaySettings(lockKey = 1).discardLockTimeout).isEqualTo(Duration.ofSeconds(30))
    }

    @Test
    fun `the discard wait must outlast the longest cycle`() {
        assertThatThrownBy {
            OutboxRelaySettings(
                lockKey = 1,
                publishTimeout = Duration.ofSeconds(2),
                maxCycleDuration = Duration.ofSeconds(10),
                discardLockTimeout = Duration.ofSeconds(11),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { OutboxRelaySettings(lockKey = 1, maxCycleDuration = Duration.ofSeconds(29)) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(
            OutboxRelaySettings(lockKey = 1, discardLockTimeout = Duration.ofSeconds(12)).discardLockTimeout,
        ).isEqualTo(Duration.ofSeconds(12))
    }
}
