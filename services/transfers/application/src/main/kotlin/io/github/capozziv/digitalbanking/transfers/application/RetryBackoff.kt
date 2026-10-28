package io.github.capozziv.digitalbanking.transfers.application

import java.time.Duration

/** Exponential wait between saga retries: `initial` * 2^(attempt-1), capped at `max`. */
class RetryBackoff(
    private val initial: Duration,
    val max: Duration,
) {
    fun delayBeforeRetry(attempt: Int): Duration {
        require(attempt >= 1) { "attempt must be >= 1, was $attempt" }
        val doublings = (attempt - 1).coerceAtMost(MAX_SHIFT)
        val delay = initial.multipliedBy(1L shl doublings)
        return if (delay > max || delay.isNegative) max else delay
    }

    private companion object {
        const val MAX_SHIFT = 20
    }
}
