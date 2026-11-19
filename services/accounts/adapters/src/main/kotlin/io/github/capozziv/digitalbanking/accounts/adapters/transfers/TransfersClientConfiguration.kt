package io.github.capozziv.digitalbanking.accounts.adapters.transfers

import io.github.capozziv.digitalbanking.accounts.application.port.TransfersGateway
import io.github.capozziv.digitalbanking.observability.CorrelationIdRestClientInterceptor
import io.github.capozziv.digitalbanking.security.ServiceTokenClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient

/** Wires the HTTP client of the `transfers` service, with the machine token and the correlation id on every call. */
@Configuration
@EnableConfigurationProperties(TransfersClientProperties::class)
class TransfersClientConfiguration {
    @Bean
    fun transfersRestClient(
        properties: TransfersClientProperties,
        serviceTokenClient: ServiceTokenClient,
    ): RestClient {
        // HTTP/1.1 only: the JDK client would otherwise try a cleartext HTTP/2 upgrade on every connection.
        val httpClient =
            HttpClient
                .newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.timeout)
                .build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient).apply { setReadTimeout(properties.timeout) }
        return RestClient
            .builder()
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .requestInterceptor(CorrelationIdRestClientInterceptor())
            .requestInterceptor(serviceTokenClient.interceptor())
            .build()
    }

    @Bean
    fun transfersGateway(
        @Qualifier("transfersRestClient") transfersRestClient: RestClient,
    ): TransfersGateway = HttpTransfersGateway(transfersRestClient)
}
