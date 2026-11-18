package io.github.capozziv.digitalbanking.security

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean

/**
 * The OpenAPI contract of a service with springdoc: [ProblemResponsesOpenApiCustomizer] documents the shared error
 * responses. The documentation routes themselves are opened by [DigitalBankingSecurityAutoConfiguration] and turned
 * off by the `aws` profile of each service (`springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false`).
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = ["org.springdoc.core.customizers.OpenApiCustomizer"])
class ApiDocsAutoConfiguration {
    @Bean
    fun problemResponsesOpenApiCustomizer() = ProblemResponsesOpenApiCustomizer()
}
