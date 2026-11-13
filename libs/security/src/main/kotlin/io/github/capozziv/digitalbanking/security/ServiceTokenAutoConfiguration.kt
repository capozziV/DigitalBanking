package io.github.capozziv.digitalbanking.security

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import java.net.URI
import java.time.Duration

/**
 * Creates the [ServiceTokenClient] of a service that calls other services, when
 * `digitalbanking.security.service-token.token-uri` is set. `client-id` and `client-secret` are then required: the
 * application does not start without them.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = ServiceTokenProperties.PREFIX, name = ["token-uri"])
@EnableConfigurationProperties(ServiceTokenProperties::class)
class ServiceTokenAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun serviceTokenClient(properties: ServiceTokenProperties) =
        ServiceTokenClient(
            tokenUri = properties.tokenUri,
            clientId = properties.clientId,
            clientSecret = properties.clientSecret,
            scope = properties.scope,
            connectTimeout = properties.connectTimeout,
            readTimeout = properties.readTimeout,
        )
}

/**
 * `digitalbanking.security.service-token.*`. Not a data class: [toString] masks the [clientSecret], so the properties
 * can show up in a log or a failure analysis without leaking it.
 */
@ConfigurationProperties(ServiceTokenProperties.PREFIX)
class ServiceTokenProperties(
    /** Token endpoint: `http://localhost:8090/default/token` locally, `https://<domain>/oauth2/token` on Cognito. */
    val tokenUri: URI,
    val clientId: String,
    val clientSecret: String,
    val scope: String = CallerResolver.SERVICE_SCOPE,
    /** Connection to the token endpoint; with [readTimeout], the worst case of a token request (2 s by default). */
    val connectTimeout: Duration = ServiceTokenClient.DEFAULT_CONNECT_TIMEOUT,
    /** From the request sent to the whole response read. */
    val readTimeout: Duration = ServiceTokenClient.DEFAULT_READ_TIMEOUT,
) {
    override fun toString() =
        "ServiceTokenProperties(tokenUri=$tokenUri, clientId=$clientId, clientSecret=****, scope=$scope, " +
            "connectTimeout=$connectTimeout, readTimeout=$readTimeout)"

    companion object {
        const val PREFIX = "digitalbanking.security.service-token"
    }
}
