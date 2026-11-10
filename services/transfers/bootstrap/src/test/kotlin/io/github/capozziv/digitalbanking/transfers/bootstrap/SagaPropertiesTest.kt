package io.github.capozziv.digitalbanking.transfers.bootstrap

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration
import org.springframework.context.annotation.Configuration
import java.time.Duration

/** `transfers.saga.*` is validated when the application starts: an invalid value stops it. */
class SagaPropertiesTest {
    private val runner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration::class.java))
            .withUserConfiguration(PropertiesConfiguration::class.java)

    @Test
    fun `the defaults are those of the spec`() {
        runner.run { context ->
            assertThat(context).hasNotFailed()
            val properties = context.getBean(SagaProperties::class.java)
            assertThat(properties.deadline).isEqualTo(Duration.ofMinutes(2))
            assertThat(properties.lease).isEqualTo(Duration.ofSeconds(30))
            assertThat(properties.busyAttempts).isEqualTo(3)
            assertThat(properties.dueBatchSize).isEqualTo(50)
            assertThat(properties.retryInitial).isEqualTo(Duration.ofSeconds(1))
            assertThat(properties.retryMax).isEqualTo(Duration.ofSeconds(15))
            assertThat(properties.pollInterval).isEqualTo(Duration.ofSeconds(1))
            assertThat(properties.schedulerEnabled).isTrue()
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "transfers.saga.busy-attempts=0",
            "transfers.saga.due-batch-size=-1",
            "transfers.saga.deadline=0s",
            "transfers.saga.lease=-1s",
            "transfers.saga.call-budget=0s",
            "transfers.saga.retry-initial=0s",
            "transfers.saga.retry-max=0s",
            "transfers.saga.poll-interval=0s",
        ],
    )
    fun `an invalid value stops the startup`(property: String) {
        val field = property.substringBefore('=').substringAfterLast('.').camelCase()
        runner.withPropertyValues(property).run { context ->
            assertThat(context).hasFailed()
            assertThat(context.startupFailure).rootCause().hasMessageContaining("on field '$field'")
        }
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "transfers.saga.call-budget=30s|transfers.saga.call-budget must be shorter than transfers.saga.lease",
            "transfers.saga.call-budget=10s,transfers.saga.lease=10s|transfers.saga.call-budget must be shorter than transfers.saga.lease",
            "transfers.saga.retry-initial=20s|transfers.saga.retry-initial must not exceed transfers.saga.retry-max",
        ],
    )
    fun `inconsistent settings stop the startup`(
        properties: String,
        message: String,
    ) {
        runner.withPropertyValues(*properties.split(',').toTypedArray()).run { context ->
            assertThat(context).hasFailed()
            assertThat(context.startupFailure).rootCause().hasMessageContaining(message)
        }
    }

    @Test
    fun `equal retry bounds and a call budget below the lease are accepted`() {
        runner
            .withPropertyValues("transfers.saga.retry-initial=5s", "transfers.saga.retry-max=5s")
            .run { context -> assertThat(context).hasNotFailed() }
    }

    private fun String.camelCase(): String {
        val parts = split('-')
        return parts.first() + parts.drop(1).joinToString("") { it.replaceFirstChar(Char::uppercase) }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SagaProperties::class)
    class PropertiesConfiguration
}
