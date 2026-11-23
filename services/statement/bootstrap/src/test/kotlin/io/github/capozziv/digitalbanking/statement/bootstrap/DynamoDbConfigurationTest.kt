package io.github.capozziv.digitalbanking.statement.bootstrap

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.core.env.StandardEnvironment
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.net.URI

/**
 * The DynamoDB endpoint is its own setting (DynamoDB Local), apart from the one of SQS (LocalStack). Each blank
 * endpoint is the real AWS, with the default credential chain and never with the test credentials.
 */
class DynamoDbConfigurationTest {
    private val runner =
        ApplicationContextRunner()
            // Without the system environment, a shell with infra/local/.env loaded cannot change the application.yml defaults.
            .withInitializer { it.environment.propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME) }
            .withInitializer(ConfigDataApplicationContextInitializer())
            .withUserConfiguration(AwsCredentialsConfiguration::class.java, DynamoDbConfiguration::class.java)

    @Test
    fun `STATEMENT_DYNAMODB_ENDPOINT sets the DynamoDB endpoint apart from the SQS one`() {
        runner.withPropertyValues("STATEMENT_AWS_ENDPOINT=$SQS", "STATEMENT_DYNAMODB_ENDPOINT=$DYNAMODB").run { context ->
            assertThat(context.getBean(DynamoDbClient::class.java).serviceClientConfiguration().endpointOverride())
                .contains(URI.create(DYNAMODB))
            assertThat(context.environment.getProperty("spring.cloud.aws.sqs.endpoint")).isEqualTo(SQS)
        }
    }

    @Test
    fun `the SQS endpoint alone leaves DynamoDB on the real AWS with the default credential chain`() {
        runner.withPropertyValues("STATEMENT_AWS_ENDPOINT=$SQS").run { context ->
            val client = context.getBean(DynamoDbClient::class.java).serviceClientConfiguration()
            assertThat(client.endpointOverride()).isEmpty
            assertThat(client.credentialsProvider()).isInstanceOf(DefaultCredentialsProvider::class.java)
            assertThat(context.getBean(AwsCredentialsProvider::class.java)).isInstanceOf(StaticCredentialsProvider::class.java)
        }
    }

    @Test
    fun `the DynamoDB endpoint alone comes with test credentials and leaves SQS on the default chain`() {
        runner.withPropertyValues("STATEMENT_DYNAMODB_ENDPOINT=$DYNAMODB").run { context ->
            val client = context.getBean(DynamoDbClient::class.java).serviceClientConfiguration()
            assertThat(client.endpointOverride()).contains(URI.create(DYNAMODB))
            assertThat(client.credentialsProvider()).isInstanceOf(StaticCredentialsProvider::class.java)
            assertThat(context.getBean(AwsCredentialsProvider::class.java)).isInstanceOf(DefaultCredentialsProvider::class.java)
        }
    }

    @Test
    fun `with the aws profile and no variables both clients target the real AWS`() {
        runner.withPropertyValues("spring.profiles.active=aws").run { context ->
            val client = context.getBean(DynamoDbClient::class.java).serviceClientConfiguration()
            assertThat(client.endpointOverride()).isEmpty
            assertThat(client.credentialsProvider()).isInstanceOf(DefaultCredentialsProvider::class.java)
            assertThat(context.getBean(AwsCredentialsProvider::class.java)).isInstanceOf(DefaultCredentialsProvider::class.java)
        }
    }

    private companion object {
        const val SQS = "http://127.0.0.1:4566"
        const val DYNAMODB = "http://127.0.0.1:8000"
    }
}
