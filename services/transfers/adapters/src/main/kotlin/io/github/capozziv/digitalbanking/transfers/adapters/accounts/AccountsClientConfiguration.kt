package io.github.capozziv.digitalbanking.transfers.adapters.accounts

import io.github.capozziv.digitalbanking.observability.CorrelationIdRestClientInterceptor
import io.github.capozziv.digitalbanking.security.ServiceTokenClient
import io.github.capozziv.digitalbanking.transfers.application.port.AccountsGateway
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient

/** Wires the HTTP client of `accounts`; the application context that includes it is assembled in `bootstrap`. */
@Configuration
@EnableConfigurationProperties(AccountsClientProperties::class)
class AccountsClientConfiguration {
    /**
     * Every call carries the correlation id of the thread and the machine token of transfers. Getting the token may
     * take up to [ServiceTokenClient.maxFetchDuration] before the request leaves; `SagaTimeoutsValidator` counts it.
     */
    @Bean
    fun accountsRestClient(
        properties: AccountsClientProperties,
        serviceTokenClient: ServiceTokenClient,
    ): RestClient {
        // HTTP/1.1 only: the JDK client would otherwise try a cleartext HTTP/2 upgrade on every connection.
        val httpClient =
            HttpClient
                .newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout)
                .build()
        // The read timeout is a total deadline per call: headers and body (proved by the stalled-body integration test).
        val requestFactory = JdkClientHttpRequestFactory(httpClient).apply { setReadTimeout(properties.readTimeout) }
        return RestClient
            .builder()
            .baseUrl(properties.url)
            .requestFactory(requestFactory)
            .requestInterceptor(CorrelationIdRestClientInterceptor())
            .requestInterceptor(serviceTokenClient.interceptor())
            .build()
    }

    @Bean
    fun accountsResponseClassifier() = AccountsResponseClassifier()

    @Bean
    fun accountsGateway(
        accountsRestClient: RestClient,
        accountsResponseClassifier: AccountsResponseClassifier,
    ): AccountsGateway = HttpAccountsGateway(accountsRestClient, accountsResponseClassifier)
}
