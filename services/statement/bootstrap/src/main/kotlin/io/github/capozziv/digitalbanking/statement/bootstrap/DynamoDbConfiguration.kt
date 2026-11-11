package io.github.capozziv.digitalbanking.statement.bootstrap

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.net.URI

/**
 * The low-level DynamoDB client (ADR 0018). A blank endpoint is the real AWS endpoint of the region; an endpoint
 * (LocalStack) overrides it. The client is AutoCloseable, so Spring closes it on shutdown.
 */
@Configuration(proxyBeanMethods = false)
class DynamoDbConfiguration {
    @Bean
    fun dynamoDbClient(
        @Value("\${statement.dynamodb.endpoint:}") endpoint: String,
        @Value("\${spring.cloud.aws.region.static}") region: String,
        credentials: AwsCredentialsProvider,
    ): DynamoDbClient =
        DynamoDbClient
            .builder()
            .region(Region.of(region))
            .credentialsProvider(credentials)
            .apply { if (endpoint.isNotBlank()) endpointOverride(URI.create(endpoint)) }
            .build()
}
