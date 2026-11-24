package io.github.capozziv.digitalbanking.observability

import io.micrometer.common.KeyValue
import io.micrometer.observation.ObservationRegistry
import org.springframework.http.client.observation.ClientHttpObservationDocumentation
import org.springframework.http.client.observation.ClientRequestObservationContext
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention
import org.springframework.web.client.RestClient

/**
 * Traces every call of this `RestClient`: a client span (and the `http.client.requests` timer) per call and the W3C
 * `traceparent` header on the request, so the trace goes on in the service called. The URL is recorded without its
 * query string ([QueryFreeClientRequestObservationConvention]).
 *
 * Needed because the clients are built with the static `RestClient.builder()`, which Spring Boot does not instrument:
 * `RestClient.builder().traced(observationRegistry).requestInterceptor(CorrelationIdRestClientInterceptor())`.
 */
fun RestClient.Builder.traced(observationRegistry: ObservationRegistry): RestClient.Builder =
    observationRegistry(observationRegistry).observationConvention(QueryFreeClientRequestObservationConvention())

/**
 * Spring's client convention, except that the `http.url` attribute leaves out the query string: a query can carry
 * personal data or keys that must not reach a span. The low-cardinality `uri` (the template) is unchanged.
 */
class QueryFreeClientRequestObservationConvention : DefaultClientRequestObservationConvention() {
    override fun requestUri(context: ClientRequestObservationContext): KeyValue {
        val uri = context.carrier?.uri ?: return super.requestUri(context)
        // On the encoded form: rebuilding the URI from its parts would encode the path a second time.
        val withoutQuery = uri.toASCIIString().substringBefore('#').substringBefore('?')
        return KeyValue.of(ClientHttpObservationDocumentation.HighCardinalityKeyNames.HTTP_URL, withoutQuery)
    }
}
