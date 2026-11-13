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
            timeout = properties.timeout,
        )
}

/** `digitalbanking.security.service-token.*`. */
@ConfigurationProperties(ServiceTokenProperties.PREFIX)
data class ServiceTokenProperties(
    /** Token endpoint: `http://localhost:8090/default/token` locally, `https://<domain>/oauth2/token` on Cognito. */
    val tokenUri: URI,
    val clientId: String,
    val clientSecret: String,
    val scope: String = CallerResolver.SERVICE_SCOPE,
    /** Connect and response timeout of the token endpoint. */
    val timeout: Duration = Duration.ofSeconds(2),
) {
    companion object {
        const val PREFIX = "digitalbanking.security.service-token"
    }
}
