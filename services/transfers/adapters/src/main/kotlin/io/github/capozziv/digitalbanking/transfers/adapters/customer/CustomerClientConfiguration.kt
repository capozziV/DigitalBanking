package io.github.capozziv.digitalbanking.transfers.adapters.customer

import io.github.capozziv.digitalbanking.observability.CorrelationIdRestClientInterceptor
import io.github.capozziv.digitalbanking.security.ServiceTokenClient
import io.github.capozziv.digitalbanking.transfers.application.port.CustomerGateway
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient

/**
 * Wires the client of `customer`. Its `RestClient` is not a bean: the accounts gateway takes the only `RestClient`
 * bean by type.
 */
@Configuration
@EnableConfigurationProperties(CustomerClientProperties::class)
class CustomerClientConfiguration {
    @Bean
    fun customerGateway(
        properties: CustomerClientProperties,
        serviceTokenClient: ServiceTokenClient,
    ): CustomerGateway {
        // HTTP/1.1 only: the JDK client would otherwise try a cleartext HTTP/2 upgrade on every connection.
        val httpClient =
            HttpClient
                .newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.timeout)
                .build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient).apply { setReadTimeout(properties.timeout) }
        val restClient =
            RestClient
                .builder()
                .baseUrl(properties.url)
                .requestFactory(requestFactory)
                .requestInterceptor(CorrelationIdRestClientInterceptor())
                .requestInterceptor(serviceTokenClient.interceptor())
                .build()
        return HttpCustomerGateway(restClient)
    }
}
