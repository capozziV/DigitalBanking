package io.github.capozziv.digitalbanking.transfers.bootstrap

import io.github.capozziv.digitalbanking.security.ServiceTokenClient
import io.github.capozziv.digitalbanking.transfers.adapters.accounts.AccountsClientProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.net.URI
import java.time.Duration

/**
 * The lease guarantee needs `transfers.saga.call-budget >= connect-timeout + read-timeout` of the `accounts` client,
 * plus the worst case of getting the machine token ([ServiceTokenClient.maxFetchDuration], 2 s by default), which the
 * interceptor may spend before the request leaves; the application does not start when the settings break it.
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
    fun `starts with a budget that covers connect, read and the service token`() {
        runner
            .withPropertyValues("transfers.accounts.read-timeout=10s", "transfers.saga.call-budget=13s")
            .run { context -> assertThat(context).hasNotFailed() }
    }

    @Test
    fun `does not start when the budget covers the call to accounts but not the service token`() {
        // Defaults: connect 1 s + read 3 s = 4 s, plus 2 s of token: 6 s.
        runner.withPropertyValues("transfers.saga.call-budget=5s").run { context ->
            assertThat(context).hasFailed()
            assertThat(context.startupFailure)
                .rootCause()
                .hasMessageContaining("transfers.saga.call-budget")
                .hasMessageContaining("service token")
                .hasMessageContaining(Duration.ofSeconds(2).toString())
        }
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
    class ValidatorConfiguration {
        /** Default timeouts (500 ms + 1.5 s); building it asks the token endpoint nothing. */
        @Bean
        fun serviceTokenClient() = ServiceTokenClient(URI.create("http://localhost:1/token"), "transfers", "secret")
    }
}
