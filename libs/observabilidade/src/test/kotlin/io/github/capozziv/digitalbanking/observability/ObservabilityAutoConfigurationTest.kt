package io.github.capozziv.digitalbanking.observability

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.annotation.ImportCandidates
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.core.Ordered

class ObservabilityAutoConfigurationTest {
    private val runner = WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration::class.java))

    @Test
    fun `the auto-configuration is listed for Spring Boot`() {
        val candidates = ImportCandidates.load(AutoConfiguration::class.java, javaClass.classLoader).candidates

        assertThat(candidates).contains(ObservabilityAutoConfiguration::class.java.name)
    }

    @Test
    fun `the correlation filter is registered ahead of every other filter`() {
        runner.run { context ->
            val registration = context.getBean("correlationIdFilterRegistration", FilterRegistrationBean::class.java)

            assertThat(registration.filter).isInstanceOf(CorrelationIdFilter::class.java)
            assertThat(registration.order).isEqualTo(Ordered.HIGHEST_PRECEDENCE)
        }
    }

    @Test
    fun `the filter is not registered outside a servlet web application`() {
        org.springframework.boot.test.context.runner
            .ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration::class.java))
            .run { context -> assertThat(context).doesNotHaveBean("correlationIdFilterRegistration") }
    }
}
