package io.github.capozziv.digitalbanking.observability

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.client.observation.ClientRequestObservationContext
import org.springframework.mock.http.client.MockClientHttpRequest
import java.net.URI

class TracedRestClientTest {
    private val convention = QueryFreeClientRequestObservationConvention()

    @Test
    fun `the http url of a client span leaves out the query string`() {
        val request = MockClientHttpRequest(HttpMethod.GET, URI.create("http://accounts:8081/accounts/a%20b/entries?idempotencyKey=k-1"))

        val keyValues = convention.getHighCardinalityKeyValues(ClientRequestObservationContext(request))

        assertThat(keyValues.single { it.key == "http.url" }.value).isEqualTo("http://accounts:8081/accounts/a%20b/entries")
    }

    @Test
    fun `a url without query is kept as it is`() {
        val request = MockClientHttpRequest(HttpMethod.GET, URI.create("http://customer:8085/internal/customers/1"))

        val keyValues = convention.getHighCardinalityKeyValues(ClientRequestObservationContext(request))

        assertThat(keyValues.single { it.key == "http.url" }.value).isEqualTo("http://customer:8085/internal/customers/1")
    }
}
