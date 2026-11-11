package io.github.capozziv.digitalbanking.statement.bootstrap

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider

/**
 * Same rule as the outbox relays and notification: with an endpoint (LocalStack), static test credentials; without
 * one (the real AWS), the AWS default credential chain. SQS and DynamoDB share STATEMENT_AWS_ENDPOINT, so the SQS
 * endpoint decides for both. Replaces the provider of Spring Cloud AWS, which backs off when this bean exists. The
 * default chain is AutoCloseable, so Spring closes it on shutdown (inferred destroy method).
 */
@Configuration(proxyBeanMethods = false)
class AwsCredentialsConfiguration {
    @Bean
    fun awsCredentialsProvider(
        @Value("\${spring.cloud.aws.sqs.endpoint:}") endpoint: String,
    ): AwsCredentialsProvider =
        if (endpoint.isBlank()) {
            DefaultCredentialsProvider.builder().build()
        } else {
            StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"))
        }
}
