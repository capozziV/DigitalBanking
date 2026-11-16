package io.github.capozziv.digitalbanking.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.annotation.ImportCandidates
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Duration

class DigitalBankingSecurityAutoConfigurationTest {
    private val runner = ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ServiceTokenAutoConfiguration::class.java))

    @Test
    fun `both auto-configurations are listed for Spring Boot`() {
        val candidates = ImportCandidates.load(AutoConfiguration::class.java, javaClass.classLoader).candidates

        assertThat(candidates).contains(
            DigitalBankingSecurityAutoConfiguration::class.java.name,
            ServiceTokenAutoConfiguration::class.java.name,
        )
    }

    @Test
    fun `without a token uri there is no service token client`() {
        runner.run { context -> assertThat(context).doesNotHaveBean(ServiceTokenClient::class.java) }
    }

    @Test
    fun `with a token uri and credentials the service token client is created`() {
        runner
            .withPropertyValues(
                "digitalbanking.security.service-token.token-uri=http://localhost:8090/default/token",
                "digitalbanking.security.service-token.client-id=transfers",
                "digitalbanking.security.service-token.client-secret=secret",
            ).run { context -> assertThat(context).hasSingleBean(ServiceTokenClient::class.java) }
    }

    @Test
    fun `the properties never print the client secret`() {
        runner
            .withPropertyValues(
                "digitalbanking.security.service-token.token-uri=http://localhost:8090/default/token",
                "digitalbanking.security.service-token.client-id=transfers",
                "digitalbanking.security.service-token.client-secret=very-secret-value",
            ).run { context ->
                val properties = context.getBean(ServiceTokenProperties::class.java)
                assertThat(properties.clientSecret).isEqualTo("very-secret-value")
                assertThat(properties.toString()).contains("transfers").doesNotContain("very-secret-value")
            }
    }

    @Test
    fun `the timeouts are configurable and default to two seconds in total`() {
        val base =
            arrayOf(
                "digitalbanking.security.service-token.token-uri=http://localhost:8090/default/token",
                "digitalbanking.security.service-token.client-id=transfers",
                "digitalbanking.security.service-token.client-secret=secret",
            )
        runner.withPropertyValues(*base).run { context ->
            assertThat(context.getBean(ServiceTokenClient::class.java).maxFetchDuration).isEqualTo(Duration.ofSeconds(2))
        }
        runner
            .withPropertyValues(
                *base,
                "digitalbanking.security.service-token.connect-timeout=200ms",
                "digitalbanking.security.service-token.read-timeout=300ms",
            ).run { context ->
                assertThat(context.getBean(ServiceTokenClient::class.java).maxFetchDuration).isEqualTo(Duration.ofMillis(500))
            }
    }

    @Test
    fun `a token uri without credentials fails at startup instead of on the first call`() {
        runner
            .withPropertyValues("digitalbanking.security.service-token.token-uri=http://localhost:8090/default/token")
            .run { context -> assertThat(context).hasFailed() }
    }
}
