package io.github.capozziv.digitalbanking.statement.adapters.cache

import io.github.capozziv.digitalbanking.statement.application.port.StatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.util.UUID
import kotlin.random.Random

/**
 * First page of the statement in Redis, as JSON under `statement:first-page:v1:{accountId}`. Redis is an
 * optimization, never a dependency: every Redis failure is logged (message only), counted and read as a miss, and
 * a value that cannot be parsed is a miss too (the next put overwrites it).
 */
class RedisStatementFirstPageCache(
    private val redis: StringRedisTemplate,
    private val mapper: JsonMapper,
    private val properties: StatementCacheProperties,
    private val meterRegistry: MeterRegistry,
    private val random: Random = Random.Default,
) : StatementFirstPageCache {
    override fun get(accountId: UUID): StatementPage? {
        val json =
            try {
                redis.opsForValue().get(key(accountId))
            } catch (e: RuntimeException) {
                failed("get", accountId, e)
                return null
            }
        if (json == null) {
            countRequest("miss")
            return null
        }
        return try {
            mapper.readValue(json, CachedStatementPage::class.java).toDomain().also { countRequest("hit") }
        } catch (e: RuntimeException) {
            log.warn("statement cache value for account {} is unreadable and counts as a miss: {}", accountId, e.message)
            countRequest("miss")
            null
        }
    }

    override fun put(
        accountId: UUID,
        page: StatementPage,
    ) {
        try {
            redis.opsForValue().set(key(accountId), mapper.writeValueAsString(CachedStatementPage.from(page)), nextTtl())
        } catch (e: RuntimeException) {
            failed("put", accountId, e)
        }
    }

    override fun evict(accountId: UUID) {
        try {
            redis.delete(key(accountId))
            meterRegistry.counter(EVICTIONS, RESULT, "ok").increment()
        } catch (e: RuntimeException) {
            log.warn("statement cache {} failed for account {}: {}", "evict", accountId, e.message)
            meterRegistry.counter(EVICTIONS, RESULT, "error").increment()
        }
    }

    /** A random TTL in `[ttl - ttlJitter, ttl + ttlJitter]`. */
    fun nextTtl(): Duration {
        val jitterMillis = properties.ttlJitter.toMillis()
        if (jitterMillis <= 0) return properties.ttl
        return properties.ttl.plusMillis(random.nextLong(-jitterMillis, jitterMillis + 1))
    }

    private fun failed(
        operation: String,
        accountId: UUID,
        e: RuntimeException,
    ) {
        log.warn("statement cache {} failed for account {}: {}", operation, accountId, e.message)
        countRequest("error")
    }

    private fun countRequest(result: String) = meterRegistry.counter(REQUESTS, RESULT, result).increment()

    private fun key(accountId: UUID) = "$KEY_PREFIX$accountId"

    private companion object {
        const val KEY_PREFIX = "statement:first-page:v1:"
        const val REQUESTS = "statement.cache.requests"
        const val EVICTIONS = "statement.cache.evictions"
        const val RESULT = "result"
        val log = LoggerFactory.getLogger(RedisStatementFirstPageCache::class.java)
    }
}
