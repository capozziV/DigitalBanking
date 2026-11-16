package io.github.capozziv.digitalbanking.observability

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder

class CorrelationIdSqsMessageInterceptorTest {
    private val interceptor = CorrelationIdSqsMessageInterceptor()

    @AfterEach
    fun clearMdc() = MDC.clear()

    private fun message(correlationId: String?): Message<Any> {
        val builder = MessageBuilder.withPayload<Any>("{}")
        if (correlationId != null) builder.setHeader(CorrelationIdSqsMessageInterceptor.MESSAGE_ATTRIBUTE, correlationId)
        return builder.build()
    }

    @Test
    fun `the correlation ID attribute goes to the MDC before the listener runs`() {
        val message = message("corr-1")

        val intercepted = interceptor.intercept(message)

        assertThat(intercepted).isSameAs(message)
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isEqualTo("corr-1")
    }

    @Test
    fun `a message without the attribute does not inherit the correlation ID of the previous one on the thread`() {
        MDC.put(CorrelationId.MDC_KEY, "stale")

        interceptor.intercept(message(null))

        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull()
    }

    @Test
    fun `a successful processing clears the MDC`() {
        val message = message("corr-ok")
        interceptor.intercept(message)

        interceptor.afterProcessing(message, null)

        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull()
    }

    @Test
    fun `a failed processing keeps the correlation ID for the container's error line that follows`() {
        val message = message("corr-fail")
        interceptor.intercept(message)

        interceptor.afterProcessing(message, IllegalStateException("boom"))

        assertThat(MDC.get(CorrelationId.MDC_KEY)).isEqualTo("corr-fail")
    }

    @Test
    fun `the message attribute is the one the outbox relay sets`() {
        assertThat(CorrelationIdSqsMessageInterceptor.MESSAGE_ATTRIBUTE).isEqualTo("correlationId")
    }
}
