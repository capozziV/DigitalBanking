package io.github.capozziv.digitalbanking.security

import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * The JWT resource server of every service.
 *
 * - The token is validated by the `JwtDecoder` Spring Boot builds from
 *   `spring.security.oauth2.resourceserver.jwt.issuer-uri` (or by a `JwtDecoder` bean, as tests do).
 * - [CallerResolver] turns it into a [Caller]; the groups claim is `digitalbanking.security.groups-claim`.
 * - The route rules are the fixed ones plus each [SecurityRules] bean (see [SecurityRules]).
 * - 401 and 403 are Problem Details (`/problems/unauthorized`, `/problems/forbidden`).
 * - The OpenAPI contract (JSON and YAML) and the Swagger UI are open ([API_DOCS_ROUTES]); the `aws` profile of each
 *   service turns springdoc off, and then these routes are 404.
 * - Stateless, no CSRF (bearer tokens only, no cookies).
 *
 * A service that declares its own `SecurityFilterChain` turns this chain off.
 */
@AutoConfiguration(
    beforeName = [
        "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
        "org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration",
        "org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration",
    ],
)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(DigitalBankingSecurityProperties::class)
@Import(SecurityExceptionHandling::class, ClientDisconnectHandling::class)
class DigitalBankingSecurityAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun callerResolver(properties: DigitalBankingSecurityProperties) = CallerResolver(groupsClaim = properties.groupsClaim)

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain::class)
    fun digitalBankingSecurityFilterChain(
        http: HttpSecurity,
        callerResolver: CallerResolver,
        rules: ObjectProvider<SecurityRules>,
    ): SecurityFilterChain {
        val entryPoint = ProblemDetailsAuthenticationEntryPoint()
        val accessDeniedHandler = ProblemDetailsAccessDeniedHandler()
        http {
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                authorize("/actuator/health/**", permitAll)
                API_DOCS_ROUTES.forEach { authorize(it, permitAll) }
                authorize("/error", permitAll)
                authorize("/admin/**", hasRole(CallerRoles.ADMIN))
                authorize("/internal/**", hasRole(CallerRoles.SERVICE))
                authorize("/actuator/**", hasRole(CallerRoles.ADMIN))
                rules.orderedStream().forEach { it.applyTo(this) }
                authorize(anyRequest, denyAll)
            }
            oauth2ResourceServer {
                jwt { jwtAuthenticationConverter = CallerAuthenticationConverter(callerResolver) }
                authenticationEntryPoint = entryPoint
                this.accessDeniedHandler = accessDeniedHandler
            }
            exceptionHandling {
                authenticationEntryPoint = entryPoint
                this.accessDeniedHandler = accessDeniedHandler
            }
        }
        return http.build()
    }

    private companion object {
        // Served by springdoc. The pattern of the JSON groups under /v3/api-docs/ does not match the YAML form.
        val API_DOCS_ROUTES = listOf("/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**", "/swagger-ui.html")
    }
}

/** `digitalbanking.security.*`. */
@ConfigurationProperties("digitalbanking.security")
data class DigitalBankingSecurityProperties(
    /** Claim with the user's groups: `groups` locally (mock-oauth2-server), `cognito:groups` on AWS. */
    val groupsClaim: String = CallerResolver.DEFAULT_GROUPS_CLAIM,
)
