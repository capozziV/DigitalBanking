package io.github.capozziv.digitalbanking.accounts.bootstrap

import io.github.capozziv.digitalbanking.outbox.OutboxRelay
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.scheduling.annotation.Scheduled
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * Runs an [OutboxRelay] cycle on a fixed delay. On shutdown no new cycle starts and the one in progress
 * is awaited (up to [shutdownTimeout]), so it commits before the data source closes: this bean depends on
 * the relay, hence on the data source, and is destroyed first.
 */
class OutboxRelayScheduler(
    private val relay: OutboxRelay,
    private val shutdownTimeout: Duration,
) : DisposableBean {
    private val cycleLock = ReentrantLock()

    @Volatile
    private var stopping = false

    @Scheduled(fixedDelayString = "\${accounts.outbox.poll-interval:500ms}")
    fun runCycle() {
        if (stopping) return
        cycleLock.lock()
        try {
            if (!stopping) relay.runCycle()
        } finally {
            cycleLock.unlock()
        }
    }

    override fun destroy() {
        stopping = true
        if (cycleLock.tryLock(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
            cycleLock.unlock()
        } else {
            log.warn("Outbox relay cycle still running after {}; shutting down without waiting for it", shutdownTimeout)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(OutboxRelayScheduler::class.java)
    }
}
