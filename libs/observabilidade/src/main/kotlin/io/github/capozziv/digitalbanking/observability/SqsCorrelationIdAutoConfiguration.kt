package io.github.capozziv.digitalbanking.observability

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean

/**
 * Registers [CorrelationIdSqsMessageInterceptor] in services that consume SQS; Spring Cloud AWS adds every
 * `MessageInterceptor` bean to its default listener container factory.
 */
@AutoConfiguration
@ConditionalOnClass(name = ["io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor"])
class SqsCorrelationIdAutoConfiguration {
    @Bean
    fun correlationIdSqsMessageInterceptor(): CorrelationIdSqsMessageInterceptor = CorrelationIdSqsMessageInterceptor()
}
