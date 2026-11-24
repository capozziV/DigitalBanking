package io.github.capozziv.digitalbanking.observability

import io.micrometer.common.KeyValue
import io.micrometer.observation.Observation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class CorrelationIdSpanAttributeTest {
    private val filter = CorrelationIdSpanAttribute()

    @AfterEach
    fun clearMdc() = MDC.remove(CorrelationId.MDC_KEY)

    @Test
    fun `the correlation id of the MDC becomes a high cardinality key, never a metric tag`() {
        MDC.put(CorrelationId.MDC_KEY, "abc")

        val context = filter.map(Observation.Context())

        assertThat(context.getHighCardinalityKeyValue("correlation.id")?.value).isEqualTo("abc")
        assertThat(context.getLowCardinalityKeyValue("correlation.id")).isNull()
    }

    @Test
    fun `without a correlation id in the MDC the observation is left as it is`() {
        val context = filter.map(Observation.Context())

        assertThat(context.highCardinalityKeyValues).isEmpty()
    }

    @Test
    fun `a correlation id the observation already has is kept`() {
        MDC.put(CorrelationId.MDC_KEY, "abc")
        val original = Observation.Context().addHighCardinalityKeyValue(KeyValue.of("correlation.id", "from-the-message"))

        val context = filter.map(original)

        assertThat(context.getHighCardinalityKeyValue("correlation.id")?.value).isEqualTo("from-the-message")
    }
}
