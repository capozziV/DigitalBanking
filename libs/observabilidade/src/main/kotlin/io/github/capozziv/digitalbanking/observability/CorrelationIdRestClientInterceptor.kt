package io.github.capozziv.digitalbanking.observability

import org.slf4j.MDC
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

/**
 * Copies the correlation ID of the current thread (MDC) to the `X-Correlation-Id` header of an outgoing call; sends
 * nothing when the MDC has none. Add it to each `RestClient` that calls another service:
 * `RestClient.builder().requestInterceptor(CorrelationIdRestClientInterceptor())`.
 */
class CorrelationIdRestClientInterceptor : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val correlationId = MDC.get(CorrelationId.MDC_KEY)
        if (!correlationId.isNullOrBlank()) request.headers.set(CorrelationId.HEADER, correlationId)
        return execution.execute(request, body)
    }
}
