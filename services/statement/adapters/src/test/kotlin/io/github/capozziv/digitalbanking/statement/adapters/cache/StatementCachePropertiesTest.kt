package io.github.capozziv.digitalbanking.statement.adapters.cache

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration

class StatementCachePropertiesTest {
    @Test
    fun `the defaults are a ttl of 60 seconds with a jitter of 10 seconds`() {
        val properties = StatementCacheProperties()

        assertThat(properties.ttl).isEqualTo(Duration.ofSeconds(60))
        assertThat(properties.ttlJitter).isEqualTo(Duration.ofSeconds(10))
    }

    @Test
    fun `a zero jitter is valid`() {
        assertThat(StatementCacheProperties(ttl = Duration.ofSeconds(5), ttlJitter = Duration.ZERO).ttlJitter).isEqualTo(Duration.ZERO)
    }

    @Test
    fun `a jitter equal to the ttl is rejected`() {
        assertThatThrownBy { StatementCacheProperties(ttl = Duration.ofSeconds(10), ttlJitter = Duration.ofSeconds(10)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("ttl-jitter")
    }

    @Test
    fun `a jitter greater than the ttl is rejected`() {
        assertThatThrownBy { StatementCacheProperties(ttl = Duration.ofSeconds(5), ttlJitter = Duration.ofSeconds(10)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `a negative jitter is rejected`() {
        assertThatThrownBy { StatementCacheProperties(ttl = Duration.ofSeconds(60), ttlJitter = Duration.ofSeconds(-1)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("ttl-jitter")
    }

    @Test
    fun `a zero or negative ttl is rejected`() {
        assertThatThrownBy { StatementCacheProperties(ttl = Duration.ZERO, ttlJitter = Duration.ZERO) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("ttl")
    }
}
