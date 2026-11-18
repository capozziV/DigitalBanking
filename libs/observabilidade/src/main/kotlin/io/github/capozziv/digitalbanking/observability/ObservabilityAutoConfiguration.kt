package io.github.capozziv.digitalbanking.observability

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/**
 * Registers [CorrelationIdFilter] in servlet applications, first of all filters (before Spring Security), and, when
 * springdoc is present, [CorrelationIdOpenApiCustomizer].
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class ObservabilityAutoConfiguration {
    @Bean
    fun correlationIdFilterRegistration(): FilterRegistrationBean<CorrelationIdFilter> =
        FilterRegistrationBean(CorrelationIdFilter()).apply { order = Ordered.HIGHEST_PRECEDENCE }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = ["org.springdoc.core.customizers.OpenApiCustomizer"])
    class ApiDocs {
        @Bean
        fun correlationIdOpenApiCustomizer() = CorrelationIdOpenApiCustomizer()
    }
}
