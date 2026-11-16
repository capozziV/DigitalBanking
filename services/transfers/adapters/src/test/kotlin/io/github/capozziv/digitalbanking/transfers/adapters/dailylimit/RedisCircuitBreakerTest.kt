package io.github.capozziv.digitalbanking.transfers.adapters.dailylimit

import io.github.capozziv.digitalbanking.transfers.adapters.dailylimit.RedisCircuitBreaker.State
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class RedisCircuitBreakerTest {
    private var now = 0L
    private val breaker = RedisCircuitBreaker(failureThreshold = 3, openDuration = Duration.ofSeconds(10)) { now }

    @Test
    fun `a closed circuit lets every call through`() {
        assertThat(breaker.tryAcquire()).isTrue()
        assertThat(breaker.tryAcquire()).isTrue()
        assertThat(breaker.state).isEqualTo(State.CLOSED)
    }

    @Test
    fun `the circuit opens after the threshold of consecutive failures and rejects calls`() {
        repeat(2) { fail() }
        assertThat(breaker.state).isEqualTo(State.CLOSED)

        fail()

        assertThat(breaker.state).isEqualTo(State.OPEN)
        assertThat(breaker.tryAcquire()).isFalse()
    }

    @Test
    fun `a success resets the count of consecutive failures`() {
        repeat(2) { fail() }
        breaker.tryAcquire()
        breaker.onSuccess()

        repeat(2) { fail() }

        assertThat(breaker.state).isEqualTo(State.CLOSED)
    }

    @Test
    fun `after the open duration a single probe goes through`() {
        repeat(3) { fail() }
        now += Duration.ofSeconds(10).toNanos() - 1
        assertThat(breaker.tryAcquire()).isFalse()

        now += 1

        assertThat(breaker.tryAcquire()).isTrue()
        assertThat(breaker.state).isEqualTo(State.HALF_OPEN)
        assertThat(breaker.tryAcquire()).isFalse()
    }

    @Test
    fun `a probe that succeeds closes the circuit`() {
        repeat(3) { fail() }
        now += Duration.ofSeconds(10).toNanos()
        breaker.tryAcquire()

        breaker.onSuccess()

        assertThat(breaker.state).isEqualTo(State.CLOSED)
        assertThat(breaker.tryAcquire()).isTrue()
    }

    @Test
    fun `a probe that fails opens the circuit for another open duration`() {
        repeat(3) { fail() }
        now += Duration.ofSeconds(10).toNanos()
        breaker.tryAcquire()

        breaker.onFailure()

        assertThat(breaker.state).isEqualTo(State.OPEN)
        now += Duration.ofSeconds(10).toNanos() - 1
        assertThat(breaker.tryAcquire()).isFalse()
        now += 1
        assertThat(breaker.tryAcquire()).isTrue()
    }

    private fun fail() {
        breaker.tryAcquire()
        breaker.onFailure()
    }
}
