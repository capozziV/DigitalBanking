package io.github.capozziv.digitalbanking.transfers.adapters.dailylimit

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
            State.CLOSED -> true
            State.HALF_OPEN -> false
            State.OPEN ->
                if (nanoTime() - openedAt >= openNanos) {
                    state = State.HALF_OPEN
                    true
                } else {
                    false
                }
        }

    @Synchronized
    fun onSuccess() {
        consecutiveFailures = 0
        state = State.CLOSED
    }

    @Synchronized
    fun onFailure() {
        consecutiveFailures++
        if (state == State.HALF_OPEN || consecutiveFailures >= failureThreshold) {
            state = State.OPEN
            openedAt = nanoTime()
        }
    }
}
