package io.github.capozziv.digitalbanking.observability

import io.micrometer.common.KeyValue
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationFilter
import org.slf4j.MDC

/**
 * Copies the correlation ID of the current thread (MDC key [CorrelationId.MDC_KEY]) to every observation as the
 * high-cardinality key [ATTRIBUTE]: the span gets it as an attribute, the metrics never get it as a tag. Runs when the
 * observation stops; the inbound HTTP observation stops inside [CorrelationIdFilter], so the ID is still in the MDC.
 * An observation that already has the key, or a thread without an ID, is left as it is.
 */
class CorrelationIdSpanAttribute : ObservationFilter {
    override fun map(context: Observation.Context): Observation.Context {
        val correlationId = MDC.get(CorrelationId.MDC_KEY)
        if (correlationId.isNullOrBlank() || context.getHighCardinalityKeyValue(ATTRIBUTE) != null) return context
        return context.addHighCardinalityKeyValue(KeyValue.of(ATTRIBUTE, correlationId))
    }

    companion object {
        const val ATTRIBUTE = "correlation.id"
    }
}
