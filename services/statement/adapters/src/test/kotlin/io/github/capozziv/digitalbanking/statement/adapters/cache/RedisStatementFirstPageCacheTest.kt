package io.github.capozziv.digitalbanking.statement.adapters.cache

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.time.Duration
import kotlin.random.Random

class RedisStatementFirstPageCacheTest {
    private val properties =
        StatementCacheProperties(enabled = true, ttl = Duration.ofSeconds(60), ttlJitter = Duration.ofSeconds(10))

    // The template is never used by nextTtl, so it needs no connection factory.
    private fun cache(
        properties: StatementCacheProperties,
        random: Random = Random.Default,
    ) = RedisStatementFirstPageCache(
        StringRedisTemplate(),
        jacksonMapperBuilder().build(),
        properties,
        SimpleMeterRegistry(),
        random,
    )

    @Test
    fun `the ttl stays within the jitter around the base ttl and varies`() {
        val cache = cache(properties, Random(42))

        val ttls = List(500) { cache.nextTtl() }

        assertThat(ttls).allSatisfy { assertThat(it).isBetween(Duration.ofSeconds(50), Duration.ofSeconds(70)) }
        assertThat(ttls.toSet().size).isGreaterThan(100)
    }

    @Test
    fun `without jitter the ttl is the base ttl`() {
        val cache = cache(properties.copy(ttlJitter = Duration.ZERO))

        assertThat(cache.nextTtl()).isEqualTo(Duration.ofSeconds(60))
    }
}
