package io.github.capozziv.digitalbanking.observability

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered

/** Registers [CorrelationIdFilter] in servlet applications, first of all filters (before Spring Security). */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class ObservabilityAutoConfiguration {
    @Bean
    fun correlationIdFilterRegistration(): FilterRegistrationBean<CorrelationIdFilter> =
        FilterRegistrationBean(CorrelationIdFilter()).apply { order = Ordered.HIGHEST_PRECEDENCE }
}
