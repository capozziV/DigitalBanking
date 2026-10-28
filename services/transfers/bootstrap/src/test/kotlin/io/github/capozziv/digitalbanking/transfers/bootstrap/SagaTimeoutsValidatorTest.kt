package io.github.capozziv.digitalbanking.transfers.bootstrap

import io.github.capozziv.digitalbanking.transfers.adapters.accounts.AccountsClientProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * The lease guarantee needs `transfers.saga.call-budget >= connect-timeout + read-timeout` of the `accounts` client;
 * the application does not start when the settings break it.
 */
class SagaTimeoutsValidatorTest {
    private val runner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration::class.java))
            .withUserConfiguration(ValidatorConfiguration::class.java)

    @Test
    fun `starts with the defaults`() {
        runner.run { context -> assertThat(context).hasNotFailed() }
    }

    @Test
    fun `starts with the values of the end-to-end tests`() {
        runner
            .withPropertyValues("transfers.accounts.read-timeout=10s", "transfers.saga.call-budget=11s")
            .run { context -> assertThat(context).hasNotFailed() }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "transfers.accounts.read-timeout=4s",
            "transfers.accounts.connect-timeout=2s",
            "transfers.saga.call-budget=3s",
        ],
    )
    fun `does not start when a call may outlast the call budget`(property: String) {
        runner.withPropertyValues(property).run { context ->
            assertThat(context).hasFailed()
            assertThat(context.startupFailure)
                .rootCause()
                .hasMessageContaining("transfers.saga.call-budget")
                .hasMessageContaining("transfers.accounts.connect-timeout")
                .hasMessageContaining("transfers.accounts.read-timeout")
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SagaProperties::class, AccountsClientProperties::class)
    @Import(SagaTimeoutsValidator::class)
    class ValidatorConfiguration
}
