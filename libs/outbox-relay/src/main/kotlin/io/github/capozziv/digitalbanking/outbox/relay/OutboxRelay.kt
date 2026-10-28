package io.github.capozziv.digitalbanking.outbox.relay

import io.github.capozziv.digitalbanking.outbox.OutboxEventEnvelope
import io.github.capozziv.digitalbanking.outbox.PendingEvent
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Publishes the outbox in position order. Only the instance holding the relay lock publishes; a failed
 * event blocks the later events of its aggregate until the next cycle, while other aggregates go on.
 * A failed event is retried after an exponential backoff (see [OutboxRelaySettings.retryBackoff]), so a broker
 * outage does not turn every cycle into a retry and a log line. A cycle stops publishing after [OutboxRelaySettings.maxCycleDuration]; the rest waits for the next one.
 */
class OutboxRelay(
    private val store: OutboxStore,
    private val publisher: EventPublisher,
    private val settings: OutboxRelaySettings,
    private val clock: Clock,
    private val metrics: OutboxMetrics,
) {
    /** Instant before which a failed event (by position) is not retried. */
    private val retryNotBefore = mutableMapOf<Long, Instant>()

    fun runCycle() {
        val ranAsLeader = store.asLeader(settings.lockKey) { publishBatch() }
        if (ranAsLeader == null) log.debug("Outbox relay lock held by another instance; skipping cycle")
        metrics.recordCycle()
        metrics.recordBacklog(store.backlog(clock.instant()))
    }

    private fun publishBatch() {
        val deadline = clock.instant().plus(settings.maxCycleDuration)
        val blockedAggregates = mutableSetOf<String>()
        for (event in store.fetchPending(settings.batchSize)) {
            if (!clock.instant().isBefore(deadline)) {
                log.warn(
                    "Outbox relay cycle exceeded its maximum duration of {}; remaining events wait for the next cycle",
                    settings.maxCycleDuration,
                )
                return
            }
            if (event.aggregateId in blockedAggregates) continue
            if (isInBackoff(event)) {
                blockedAggregates += event.aggregateId
                continue
            }
            withCorrelationId(event.correlationId) {
                if (!publish(event)) blockedAggregates += event.aggregateId
            }
        }
    }

    /** A broker failure is recorded on the event; a database failure propagates and rolls the cycle back. */
    private fun publish(event: PendingEvent): Boolean {
        try {
            publisher.publish(event)
        } catch (e: Exception) {
            store.markFailed(event.position, describe(e))
            metrics.recordPublishFailure()
            logFailure(event, e)
            retryNotBefore[event.position] = clock.instant().plus(backoff(event.attempts + 1))
            return false
        }
        store.markPublished(event.position, clock.instant())
        retryNotBefore.remove(event.position)
        return true
    }

    private fun isInBackoff(event: PendingEvent): Boolean {
        val notBefore = retryNotBefore[event.position] ?: return false
        return clock.instant().isBefore(notBefore)
    }

    private fun backoff(attempts: Int): Duration {
        val doublings = (attempts - 1).coerceIn(0, MAX_DOUBLINGS)
        return minOf(settings.retryBackoff.multipliedBy(1L shl doublings), settings.maxRetryBackoff)
    }

    private fun logFailure(
        event: PendingEvent,
        error: Exception,
    ) {
        val attempts = event.attempts + 1
        val message =
            "Outbox event publication failed; later events of the aggregate wait for the next cycle " +
                "(position={}, eventId={}, aggregateId={}, eventType={}, attempts={}, error={})"
        val arguments = arrayOf<Any>(event.position, event.eventId, event.aggregateId, event.eventType, attempts, describe(error))
        if (attempts == settings.alertAfterAttempts) {
            log.error(message, *arguments, error)
        } else {
            log.warn(message, *arguments)
        }
    }

    private fun describe(error: Exception): String = "${error.javaClass.name}: ${error.message}".take(MAX_ERROR_LENGTH)

    private inline fun withCorrelationId(
        correlationId: String?,
        block: () -> Unit,
    ) {
        val previous = MDC.get(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY)
        if (correlationId == null) {
            MDC.remove(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY)
        } else {
            MDC.put(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY, correlationId)
        }
        try {
            block()
        } finally {
            if (previous == null) {
                MDC.remove(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY)
            } else {
                MDC.put(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY, previous)
            }
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(OutboxRelay::class.java)
        const val MAX_ERROR_LENGTH = 2000
        const val MAX_DOUBLINGS = 20
    }
}
