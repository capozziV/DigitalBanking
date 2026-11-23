package io.github.capozziv.digitalbanking.statement.adapters.cache

import io.github.capozziv.digitalbanking.statement.application.port.FirstPageLookup
import io.github.capozziv.digitalbanking.statement.application.port.StatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.util.UUID
import kotlin.random.Random

/**
 * First page of the statement in Redis, as JSON under `statement:first-page:v1:{accountId}`. Redis is an
 * optimization, never a dependency: every Redis failure is logged (message only), counted and read as a miss, and
 * a value that cannot be parsed is a miss too (the next put overwrites it).
 *
 * Each eviction bumps a per-account version (`statement:first-page-version:v1:{accountId}`); a miss returns the
 * version it read and the put stores the page only if the version is unchanged, checked in the same Lua script, so a
 * page read from DynamoDB before an eviction never lands in the cache after it (#226).
 */
class RedisStatementFirstPageCache(
    private val redis: StringRedisTemplate,
    private val mapper: JsonMapper,
    private val properties: StatementCacheProperties,
    private val meterRegistry: MeterRegistry,
    private val random: Random = Random.Default,
) : StatementFirstPageCache {
    override fun get(accountId: UUID): FirstPageLookup {
        val values =
            try {
                // One MGET: the page and the version are read at the same instant.
                redis.opsForValue().multiGet(listOf(key(accountId), versionKey(accountId)))
            } catch (e: RuntimeException) {
                failed("get", accountId, e) { countRequest("error") }
                return FirstPageLookup.Unavailable
            }
        val json = values?.getOrNull(0)
        val version = values?.getOrNull(1)?.toLongOrNull() ?: 0
        if (json == null) {
            countRequest("miss")
            return FirstPageLookup.Miss(version)
        }
        return try {
            FirstPageLookup.Hit(mapper.readValue(json, CachedStatementPage::class.java).toDomain()).also { countRequest("hit") }
        } catch (e: RuntimeException) {
            log.warn("statement cache value for account {} is unreadable and counts as a miss: {}", accountId, e.message)
            countRequest("miss")
            FirstPageLookup.Miss(version)
        }
    }

    override fun put(
        accountId: UUID,
        page: StatementPage,
        version: Long,
    ) {
        try {
            val stored =
                redis.execute(
                    PUT_IF_VERSION,
                    listOf(key(accountId), versionKey(accountId)),
                    version.toString(),
                    mapper.writeValueAsString(CachedStatementPage.from(page)),
                    nextTtl().toMillis().toString(),
                )
            countWrite(if (stored == 1L) "ok" else "stale")
        } catch (e: RuntimeException) {
            failed("put", accountId, e) { countWrite("error") }
        }
    }

    override fun evict(accountId: UUID) {
        try {
            redis.execute(EVICT, listOf(key(accountId), versionKey(accountId)), VERSION_TTL.toMillis().toString())
            meterRegistry.counter(EVICTIONS, RESULT, "ok").increment()
        } catch (e: RuntimeException) {
            failed("evict", accountId, e) { meterRegistry.counter(EVICTIONS, RESULT, "error").increment() }
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
        count: () -> Unit,
    ) {
        log.warn("statement cache {} failed for account {}: {}", operation, accountId, e.message)
        count()
    }

    private fun countWrite(result: String) = meterRegistry.counter(WRITES, RESULT, result).increment()

    private fun countRequest(result: String) = meterRegistry.counter(REQUESTS, RESULT, result).increment()

    private fun key(accountId: UUID) = "$KEY_PREFIX$accountId"

    private fun versionKey(accountId: UUID) = "$VERSION_KEY_PREFIX$accountId"

    private companion object {
        const val KEY_PREFIX = "statement:first-page:v1:"
        const val VERSION_KEY_PREFIX = "statement:first-page-version:v1:"

        /**
         * Far longer than any read between its miss and its put. When the version key expires it reads as 0 again;
         * a read could only be fooled if it lasted from before an eviction until after this expiry.
         */
        val VERSION_TTL: Duration = Duration.ofDays(1)

        /** KEYS: page, version. ARGV: version read with the miss, page JSON, TTL in ms. 1 = stored, 0 = stale. */
        val PUT_IF_VERSION: RedisScript<Long> =
            RedisScript.of(
                """
                if (redis.call('GET', KEYS[2]) or '0') ~= ARGV[1] then return 0 end
                redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
                return 1
                """.trimIndent(),
                Long::class.javaObjectType,
            )

        /** KEYS: page, version. ARGV: version TTL in ms. Bumps the version, then drops the page, atomically. */
        val EVICT: RedisScript<Long> =
            RedisScript.of(
                """
                redis.call('INCR', KEYS[2])
                redis.call('PEXPIRE', KEYS[2], ARGV[1])
                redis.call('DEL', KEYS[1])
                return 1
                """.trimIndent(),
                Long::class.javaObjectType,
            )
        const val REQUESTS = "statement.cache.requests"
        const val WRITES = "statement.cache.writes"
        const val EVICTIONS = "statement.cache.evictions"
        const val RESULT = "result"
        val log = LoggerFactory.getLogger(RedisStatementFirstPageCache::class.java)
    }
}
