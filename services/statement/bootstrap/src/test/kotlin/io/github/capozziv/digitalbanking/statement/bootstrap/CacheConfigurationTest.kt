package io.github.capozziv.digitalbanking.statement.bootstrap

import io.github.capozziv.digitalbanking.statement.adapters.cache.NoOpStatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.adapters.cache.RedisStatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.application.port.StatementFirstPageCache
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import tools.jackson.databind.json.JsonMapper

class CacheConfigurationTest {
    // The Redis template is never used by the beans under test, so its connection factory is a mock.
    @Configuration(proxyBeanMethods = false)
    class Collaborators {
        @Bean
        fun redisTemplate() = StringRedisTemplate(mock(RedisConnectionFactory::class.java))

        @Bean
        fun jsonMapper(): JsonMapper = JsonMapper.builder().build()

        @Bean
        fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()
    }

    private val runner = ApplicationContextRunner().withUserConfiguration(CacheConfiguration::class.java, Collaborators::class.java)

    @Test
    fun `the cache is the Redis one by default`() {
        runner.run { context ->
            assertThat(context).hasSingleBean(StatementFirstPageCache::class.java)
            assertThat(context.getBean(StatementFirstPageCache::class.java)).isInstanceOf(RedisStatementFirstPageCache::class.java)
        }
    }

    @Test
    fun `the cache is the Redis one when enabled`() {
        runner.withPropertyValues("statement.cache.enabled=true").run { context ->
            assertThat(context.getBean(StatementFirstPageCache::class.java)).isInstanceOf(RedisStatementFirstPageCache::class.java)
        }
    }

    @Test
    fun `with statement cache enabled false the cache is the no-op one`() {
        runner.withPropertyValues("statement.cache.enabled=false").run { context ->
            assertThat(context).hasSingleBean(StatementFirstPageCache::class.java)
            assertThat(context.getBean(StatementFirstPageCache::class.java)).isInstanceOf(NoOpStatementFirstPageCache::class.java)
        }
    }
}
