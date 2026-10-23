package io.github.capozziv.digitalbanking.outbox

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Clock

/**
 * Publishes the outbox in position order. Only the instance holding the relay lock publishes; a failed
 * event blocks the later events of its aggregate until the next cycle, while other aggregates go on.
 */
class OutboxRelay(
    private val store: OutboxStore,
    private val publisher: EventPublisher,
    private val settings: OutboxRelaySettings,
    private val clock: Clock,
    private val metrics: OutboxMetrics,
) {
    fun runCycle() {
        val ranAsLeader = store.asLeader(settings.lockKey) { publishBatch() }
        if (ranAsLeader == null) log.debug("Outbox relay lock held by another instance; skipping cycle")
        metrics.recordBacklog(store.backlog(clock.instant()))
    }

    private fun publishBatch() {
        val blockedAggregates = mutableSetOf<String>()
        for (event in store.fetchPending(settings.batchSize)) {
            if (event.aggregateId in blockedAggregates) continue
            withCorrelationId(event.correlationId) {
                if (!tryPublish(event)) blockedAggregates += event.aggregateId
            }
        }
    }

    private fun tryPublish(event: PendingEvent): Boolean =
        try {
            publisher.publish(event)
            store.markPublished(event.position, clock.instant())
            true
        } catch (e: Exception) {
            store.markFailed(event.position, describe(e))
            metrics.recordPublishFailure()
            logFailure(event, e)
            false
        }

    private fun logFailure(
        event: PendingEvent,
        error: Exception,
    ) {
        val attempts = event.attempts + 1
        val message =
            "Outbox event publication failed; later events of the aggregate wait for the next cycle " +
                "(position={}, eventId={}, aggregateId={}, eventType={}, attempts={})"
        val arguments = arrayOf(event.position, event.eventId, event.aggregateId, event.eventType, attempts, error)
        if (attempts >= settings.alertAfterAttempts) {
            log.error(message, *arguments)
        } else {
            log.warn(message, *arguments)
        }
    }

    private fun describe(error: Exception): String = "${error.javaClass.name}: ${error.message}".take(MAX_ERROR_LENGTH)

    private inline fun withCorrelationId(
        correlationId: String?,
        block: () -> Unit,
    ) {
        val previous = MDC.get(OutboxEnvelope.CORRELATION_ID_MDC_KEY)
        if (correlationId == null) {
            MDC.remove(OutboxEnvelope.CORRELATION_ID_MDC_KEY)
        } else {
            MDC.put(OutboxEnvelope.CORRELATION_ID_MDC_KEY, correlationId)
        }
        try {
            block()
        } finally {
            if (previous == null) {
                MDC.remove(OutboxEnvelope.CORRELATION_ID_MDC_KEY)
            } else {
                MDC.put(OutboxEnvelope.CORRELATION_ID_MDC_KEY, previous)
            }
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(OutboxRelay::class.java)
        const val MAX_ERROR_LENGTH = 2000
    }
}
