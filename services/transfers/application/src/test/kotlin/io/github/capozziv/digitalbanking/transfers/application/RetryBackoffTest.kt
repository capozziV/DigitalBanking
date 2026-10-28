package io.github.capozziv.digitalbanking.transfers.application

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration

class RetryBackoffTest {
    private val backoff = RetryBackoff(initial = Duration.ofSeconds(1), max = Duration.ofSeconds(15))

    @Test
    fun `delay doubles per attempt and is capped`() {
        val delays = (1..6).map { backoff.delayBeforeRetry(it).seconds }
        assertThat(delays).containsExactly(1L, 2L, 4L, 8L, 15L, 15L)
    }

    @Test
    fun `huge attempt counts stay capped without overflow`() {
        assertThat(backoff.delayBeforeRetry(10_000)).isEqualTo(Duration.ofSeconds(15))
    }

    @Test
    fun `exposes the maximum wait`() {
        assertThat(backoff.max).isEqualTo(Duration.ofSeconds(15))
    }

    @Test
    fun `attempt zero is rejected`() {
        assertThatThrownBy { backoff.delayBeforeRetry(0) }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
