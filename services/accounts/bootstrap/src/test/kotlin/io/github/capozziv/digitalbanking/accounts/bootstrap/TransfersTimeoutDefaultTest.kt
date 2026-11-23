package io.github.capozziv.digitalbanking.accounts.bootstrap

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.convert.DurationStyle
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.MutablePropertySources
import org.springframework.core.env.PropertySourcesPropertyResolver
import org.springframework.core.io.ClassPathResource
import java.time.Duration

/**
 * The integration tests override accounts.transfers.timeout to 2s (base class), so they cannot see the production default.
 * This test reads application.yml directly, with no override and no environment variable, and pins the default at 500 ms.
 */
class TransfersTimeoutDefaultTest {
    @Test
    fun `the timeout of the transfers service is 500 ms by default`() {
        val sources = MutablePropertySources()
        YamlPropertySourceLoader().load("application", ClassPathResource("application.yml")).forEach(sources::addLast)
        val resolver = PropertySourcesPropertyResolver(sources)

        val timeout = DurationStyle.detectAndParse(resolver.getRequiredProperty("accounts.transfers.timeout"))

        assertThat(timeout).isEqualTo(Duration.ofMillis(500))
    }
}
