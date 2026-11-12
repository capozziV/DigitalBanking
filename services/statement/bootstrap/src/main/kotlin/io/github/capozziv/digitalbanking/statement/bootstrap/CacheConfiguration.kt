package io.github.capozziv.digitalbanking.statement.bootstrap

import io.github.capozziv.digitalbanking.statement.adapters.cache.NoOpStatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.adapters.cache.RedisStatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.adapters.cache.StatementCacheProperties
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate
import tools.jackson.databind.json.JsonMapper

/** Chooses the first-page cache: Redis, unless `statement.cache.enabled=false` selects the no-op one. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StatementCacheProperties::class)
class CacheConfiguration {
    @Bean
    @ConditionalOnProperty("statement.cache.enabled", havingValue = "true", matchIfMissing = true)
    fun redisStatementFirstPageCache(
        redis: StringRedisTemplate,
        mapper: JsonMapper,
        properties: StatementCacheProperties,
        meterRegistry: MeterRegistry,
    ) = RedisStatementFirstPageCache(redis, mapper, properties, meterRegistry)

    @Bean
    @ConditionalOnProperty("statement.cache.enabled", havingValue = "false")
    fun noOpStatementFirstPageCache() = NoOpStatementFirstPageCache()
}
