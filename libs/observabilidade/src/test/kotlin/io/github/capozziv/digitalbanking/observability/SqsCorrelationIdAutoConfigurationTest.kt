package io.github.capozziv.digitalbanking.observability

import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.annotation.ImportCandidates
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class SqsCorrelationIdAutoConfigurationTest {
    private val runner = ApplicationContextRunner().withConfiguration(AutoConfigurations.of(SqsCorrelationIdAutoConfiguration::class.java))

    @Test
    fun `the auto-configuration is listed for Spring Boot`() {
        val candidates = ImportCandidates.load(AutoConfiguration::class.java, javaClass.classLoader).candidates

        assertThat(candidates).contains(SqsCorrelationIdAutoConfiguration::class.java.name)
    }

    @Test
    fun `the interceptor is a MessageInterceptor bean, which the SQS container factory picks up`() {
        runner.run { context ->
            assertThat(context.getBeansOfType(MessageInterceptor::class.java).values)
                .singleElement()
                .isInstanceOf(CorrelationIdSqsMessageInterceptor::class.java)
        }
    }

    @Test
    fun `nothing is registered when the service does not use Spring Cloud AWS SQS`() {
        runner
            .withClassLoader(FilteredClassLoader(MessageInterceptor::class.java))
            .run { context -> assertThat(context).doesNotHaveBean(CorrelationIdSqsMessageInterceptor::class.java) }
    }
}
