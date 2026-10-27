package io.github.capozziv.digitalbanking.transfers.adapters.correlation

import io.github.capozziv.digitalbanking.outbox.OutboxEventEnvelope.CORRELATION_ID_MDC_KEY
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class MdcCorrelationContextTest {
    private val context = MdcCorrelationContext()

    @AfterEach
    fun clearMdc() {
        MDC.clear()
    }

    @Test
    fun `the block sees the correlation id in the MDC and it is removed afterwards`() {
        val seen = context.withCorrelationId("corr-1") { MDC.get(CORRELATION_ID_MDC_KEY) }

        assertThat(seen).isEqualTo("corr-1")
        assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull()
    }

    @Test
    fun `a null id runs the block without a correlation id and restores the previous one`() {
        MDC.put(CORRELATION_ID_MDC_KEY, "outer")

        val seen = context.withCorrelationId(null) { MDC.get(CORRELATION_ID_MDC_KEY) }

        assertThat(seen).isNull()
        assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isEqualTo("outer")
    }

    @Test
    fun `the previous id comes back when the block throws`() {
        MDC.put(CORRELATION_ID_MDC_KEY, "outer")

        assertThatThrownBy { context.withCorrelationId("inner") { error("boom") } }.hasMessage("boom")

        assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isEqualTo("outer")
    }
}
