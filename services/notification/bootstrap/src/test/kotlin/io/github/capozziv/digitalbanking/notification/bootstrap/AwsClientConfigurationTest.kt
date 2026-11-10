package io.github.capozziv.digitalbanking.notification.bootstrap

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration
import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.net.URI

/**
 * The SQS client built from `application.yml`, aligned with the outbox relays: a blank endpoint means the real AWS
 * endpoint and the AWS default credential chain; an endpoint (LocalStack) comes with static test credentials.
 */
class AwsClientConfigurationTest {
    private val runner =
        ApplicationContextRunner()
            .withInitializer(ConfigDataApplicationContextInitializer())
            .withUserConfiguration(AwsCredentialsConfiguration::class.java)
            .withConfiguration(
                AutoConfigurations.of(
                    AwsAutoConfiguration::class.java,
                    RegionProviderAutoConfiguration::class.java,
                    CredentialsProviderAutoConfiguration::class.java,
                    SqsAutoConfiguration::class.java,
                ),
            )

    @Test
    fun `without NOTIFICATION_AWS_ENDPOINT the client targets the real AWS with the default credential chain`() {
        // A shell with infra/local/.env loaded defines it; the default of application.yml is what is under test.
        assumeTrue(System.getenv("NOTIFICATION_AWS_ENDPOINT") == null, "NOTIFICATION_AWS_ENDPOINT defined in the environment")

        runner.run { context ->
            assertThat(context.getBean(SqsAsyncClient::class.java).serviceClientConfiguration().endpointOverride()).isEmpty
            assertThat(context.getBean(AwsCredentialsProvider::class.java)).isInstanceOf(DefaultCredentialsProvider::class.java)
        }
    }

    @Test
    fun `a blank endpoint means the real AWS with the default credential chain`() {
        runner.withPropertyValues("spring.cloud.aws.sqs.endpoint=").run { context ->
            assertThat(context.getBean(SqsAsyncClient::class.java).serviceClientConfiguration().endpointOverride()).isEmpty
            assertThat(context.getBean(AwsCredentialsProvider::class.java)).isInstanceOf(DefaultCredentialsProvider::class.java)
        }
    }

    @Test
    fun `an endpoint targets LocalStack with static test credentials`() {
        runner.withPropertyValues("spring.cloud.aws.sqs.endpoint=http://127.0.0.1:4566").run { context ->
            assertThat(context.getBean(SqsAsyncClient::class.java).serviceClientConfiguration().endpointOverride())
                .contains(URI.create("http://127.0.0.1:4566"))
            val credentials = context.getBean(AwsCredentialsProvider::class.java)
            assertThat(credentials).isInstanceOf(StaticCredentialsProvider::class.java)
            assertThat(credentials.resolveCredentials().accessKeyId()).isEqualTo("test")
        }
    }
}
