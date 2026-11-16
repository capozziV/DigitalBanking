package io.github.capozziv.digitalbanking.observability

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.http.HttpMethod
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class CorrelationIdRestClientInterceptorTest {
    private val builder = RestClient.builder().requestInterceptor(CorrelationIdRestClientInterceptor())
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val client = builder.build()

    @AfterEach
    fun cleanMdc() = MDC.remove(CorrelationId.MDC_KEY)

    @Test
    fun `the correlation id in the MDC goes in the outgoing header`() {
        MDC.put(CorrelationId.MDC_KEY, "abc-123")
        server
            .expect(ExpectedCount.once(), requestTo("http://peer.test/x"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(CorrelationId.HEADER, "abc-123"))
            .andRespond(withSuccess())

        client
            .get()
            .uri("http://peer.test/x")
            .retrieve()
            .toBodilessEntity()

        server.verify()
    }

    @Test
    fun `without a correlation id in the MDC no header is sent`() {
        server
            .expect(ExpectedCount.once(), requestTo("http://peer.test/x"))
            .andExpect(headerDoesNotExist(CorrelationId.HEADER))
            .andRespond(withSuccess())

        client
            .get()
            .uri("http://peer.test/x")
            .retrieve()
            .toBodilessEntity()

        server.verify()
    }
}
