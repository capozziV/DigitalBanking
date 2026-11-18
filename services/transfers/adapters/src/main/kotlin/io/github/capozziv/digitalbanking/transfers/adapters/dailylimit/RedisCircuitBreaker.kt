package io.github.capozziv.digitalbanking.transfers.adapters.dailylimit

import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Circuit breaker of the Redis commands of the daily limit, so that with Redis down each reservation goes straight to
 * PostgreSQL instead of paying the command timeout first.
 *
 * [failureThreshold] consecutive failures open the circuit; after [openDuration] one call (the probe) goes through in
 * HALF_OPEN while the others are still rejected. The probe's success closes the circuit, its failure opens it again.
 * Every call that [tryAcquire] lets through must end in [onSuccess] or [onFailure].
 */
class RedisCircuitBreaker(
    private val failureThreshold: Int,
    openDuration: Duration,
    private val nanoTime: () -> Long = System::nanoTime,
) {
    enum class State(
        /** The value of the state gauge. */
        val code: Int,
    ) {
        CLOSED(0),
        OPEN(1),
        HALF_OPEN(2),
    }

    private val openNanos = openDuration.toNanos()
    private var consecutiveFailures = 0
    private var openedAt = 0L

    @Volatile
    var state: State = State.CLOSED
        private set

    init {
        require(failureThreshold > 0) { "failureThreshold must be positive, was $failureThreshold" }
        require(openNanos > 0) { "openDuration must be positive, was $openDuration" }
    }

    /** True when the call may go to Redis; false when it must take the fallback right away. */
    @Synchronized
    fun tryAcquire(): Boolean =
        when (state) {
            State.CLOSED -> {
                true
            }

            State.HALF_OPEN -> {
                false
            }

            State.OPEN -> {
                if (nanoTime() - openedAt >= openNanos) {
                    transitionTo(State.HALF_OPEN)
                    true
                } else {
                    false
                }
            }
        }

    /** Only a success while CLOSED (resets the count) or the probe's, in HALF_OPEN, counts; a late one in OPEN is ignored. */
    @Synchronized
    fun onSuccess() {
        when (state) {
            State.CLOSED -> {
                consecutiveFailures = 0
            }

            State.HALF_OPEN -> {
                consecutiveFailures = 0
                transitionTo(State.CLOSED)
            }

            State.OPEN -> {
                Unit
            }
        }
    }

    /** A failure of a call that ends after the circuit opened is ignored: it must not extend the open duration. */
    @Synchronized
    fun onFailure() {
        when (state) {
            State.CLOSED -> {
                consecutiveFailures++
                if (consecutiveFailures >= failureThreshold) open()
            }

            State.HALF_OPEN -> {
                open()
            }

            State.OPEN -> {
                Unit
            }
        }
    }

    /** Opens the circuit and starts the count over, so the next closing needs the threshold of failures again. */
    private fun open() {
        consecutiveFailures = 0
        openedAt = nanoTime()
        transitionTo(State.OPEN)
    }

    private fun transitionTo(next: State) {
        val previous = state
        state = next
        if (next == State.OPEN) {
            log.warn("daily limit redis circuit breaker {} -> {}: reservations go to postgres", previous, next)
        } else {
            log.info("daily limit redis circuit breaker {} -> {}", previous, next)
        }
    }

    private companion object {
        private val log = LoggerFactory.getLogger(RedisCircuitBreaker::class.java)
    }
}
