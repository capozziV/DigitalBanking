package io.github.capozziv.digitalbanking.transfers.bootstrap

import io.github.capozziv.digitalbanking.transfers.application.usecase.ResumeDueTransfersUseCase
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.scheduling.annotation.Scheduled
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * Resumes the due transfers on a fixed delay. On shutdown no new tick starts, the one in progress finishes the
 * transfer it is on without starting the next one, and is awaited (up to [shutdownTimeout]), so its writes commit before the data source closes: this bean depends on the use case,
 * hence on the data source, and is destroyed first. A tick cut short is safe: every write is guarded by the lease,
 * and the calls to `accounts` are idempotent, so another run resumes the transfer once the lease expires.
 */
class TransferSagaScheduler(
    private val resumeDue: ResumeDueTransfersUseCase,
    private val shutdownTimeout: Duration,
) : DisposableBean {
    private val tickLock = ReentrantLock()

    @Volatile
    private var stopping = false

    @Scheduled(fixedDelayString = "\${transfers.saga.poll-interval:1s}")
    fun runTick() {
        if (stopping) return
        tickLock.lock()
        try {
            if (!stopping) resumeDue.execute { !stopping }
        } finally {
            tickLock.unlock()
        }
    }

    override fun destroy() {
        stopping = true
        if (tickLock.tryLock(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
            tickLock.unlock()
        } else {
            log.warn("Transfer saga tick still running after {}; shutting down without waiting for it", shutdownTimeout)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(TransferSagaScheduler::class.java)
    }
}
