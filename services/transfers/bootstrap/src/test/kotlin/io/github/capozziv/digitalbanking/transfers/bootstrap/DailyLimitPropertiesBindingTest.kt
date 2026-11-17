package io.github.capozziv.digitalbanking.transfers.bootstrap

import io.github.capozziv.digitalbanking.transfers.adapters.dailylimit.DailyLimitProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.SystemEnvironmentPropertySource
import org.springframework.core.io.ClassPathResource
import java.time.Duration

/** `transfers.daily-limit.*` of the real `application.yml`: the defaults it declares and the environment variables. */
class DailyLimitPropertiesBindingTest {
    @Test
    fun `application yml declares the defaults of the daily limit`() {
        val properties = bind(emptyMap())

        assertThat(properties).isEqualTo(DailyLimitProperties())
        assertThat(properties.lockTimeout).isEqualTo(Duration.ofSeconds(2))
        assertThat(properties.circuitBreaker.failureThreshold).isEqualTo(5)
        assertThat(properties.circuitBreaker.openDuration).isEqualTo(Duration.ofSeconds(10))
    }

    @Test
    fun `each property of the daily limit is bound to its environment variable`() {
        val properties =
            bind(
                mapOf(
                    "TRANSFERS_DAILY_LIMIT" to "123400",
                    "TRANSFERS_DAILY_LIMIT_COUNTER_TTL" to "7m",
                    "TRANSFERS_DAILY_LIMIT_LOCK_TIMEOUT" to "750ms",
                    "TRANSFERS_DAILY_LIMIT_CIRCUIT_BREAKER_FAILURE_THRESHOLD" to "9",
                    "TRANSFERS_DAILY_LIMIT_CIRCUIT_BREAKER_OPEN_DURATION" to "42s",
                ),
            )

        assertThat(properties.amount).isEqualTo(123_400)
        assertThat(properties.counterTtl).isEqualTo(Duration.ofMinutes(7))
        assertThat(properties.lockTimeout).isEqualTo(Duration.ofMillis(750))
        assertThat(properties.circuitBreaker.failureThreshold).isEqualTo(9)
        assertThat(properties.circuitBreaker.openDuration).isEqualTo(Duration.ofSeconds(42))
    }

    private fun bind(environmentVariables: Map<String, Any>): DailyLimitProperties {
        val environment = StandardEnvironment()
        environment.propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)
        environment.propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
        YamlPropertySourceLoader()
            .load("application", ClassPathResource("application.yml"))
            .forEach { environment.propertySources.addLast(it) }
        environment.propertySources.addFirst(SystemEnvironmentPropertySource("test-env", environmentVariables))
        return Binder.get(environment).bind("transfers.daily-limit", DailyLimitProperties::class.java).get()
    }
}
