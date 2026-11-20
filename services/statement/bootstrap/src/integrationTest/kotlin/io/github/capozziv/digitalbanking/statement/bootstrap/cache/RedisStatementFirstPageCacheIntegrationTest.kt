package io.github.capozziv.digitalbanking.statement.bootstrap.cache

import io.github.capozziv.digitalbanking.statement.adapters.cache.RedisStatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.application.port.FirstPageLookup
import io.github.capozziv.digitalbanking.statement.bootstrap.StatementIntegrationTest
import io.github.capozziv.digitalbanking.statement.domain.EntryKind
import io.github.capozziv.digitalbanking.statement.domain.StatementCursor
import io.github.capozziv.digitalbanking.statement.domain.StatementEntry
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import io.github.capozziv.digitalbanking.testing.redis.RedisTestSupport
import io.micrometer.core.instrument.MeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Instant
import java.util.UUID
import kotlin.system.measureTimeMillis

class RedisStatementFirstPageCacheIntegrationTest : StatementIntegrationTest() {
    @Autowired
    private lateinit var cache: RedisStatementFirstPageCache

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    private fun entry(
        accountId: UUID,
        sequence: Long,
        kind: EntryKind,
        balanceAfterCents: Long,
        description: String?,
    ) = StatementEntry(
        entryId = UUID.randomUUID(),
        accountId = accountId,
        sequence = sequence,
        kind = kind,
        amountCents = 1_234,
        balanceAfterCents = balanceAfterCents,
        description = description,
        createdAt = Instant.parse("2026-11-12T10:15:30.123456Z"),
    )

    private fun counter(
        name: String,
        result: String,
    ): Double =
        meterRegistry
            .find(name)
            .tag("result", result)
            .counter()
            ?.count() ?: 0.0

    private fun key(accountId: UUID) = "statement:first-page:v1:$accountId"

    private fun versionKey(accountId: UUID) = "statement:first-page-version:v1:$accountId"

    @Test
    fun `a stored page is read back equal`() {
        val accountId = UUID.randomUUID()
        val page =
            StatementPage(
                entries =
                    listOf(
                        entry(accountId, 5, EntryKind.DEBIT, -500, null),
                        entry(accountId, 4, EntryKind.CREDIT, 734, "salary"),
                    ),
                nextCursor = StatementCursor(4),
            )
        val empty = StatementPage(emptyList(), null)
        val emptyAccountId = UUID.randomUUID()

        cache.put(accountId, page, 0)
        cache.put(emptyAccountId, empty, 0)

        assertThat(cache.get(accountId)).isEqualTo(FirstPageLookup.Hit(page))
        assertThat(cache.get(emptyAccountId)).isEqualTo(FirstPageLookup.Hit(empty))
    }

    @Test
    fun `a miss carries the version and each eviction bumps it`() {
        val accountId = UUID.randomUUID()

        assertThat(cache.get(accountId)).isEqualTo(FirstPageLookup.Miss(0))
        cache.evict(accountId)
        cache.evict(accountId)

        assertThat(cache.get(accountId)).isEqualTo(FirstPageLookup.Miss(2))
        assertThat(redisTemplate.getExpire(versionKey(accountId))).isGreaterThan(70L)
    }

    @Test
    fun `a put with the version of its miss is stored`() {
        val accountId = UUID.randomUUID()
        val page = StatementPage(emptyList(), null)
        cache.evict(accountId)
        val miss = cache.get(accountId) as FirstPageLookup.Miss

        cache.put(accountId, page, miss.version)

        assertThat(cache.get(accountId)).isEqualTo(FirstPageLookup.Hit(page))
    }

    @Test
    fun `a put after an eviction that followed its miss is dropped and counted as stale`() {
        val accountId = UUID.randomUUID()
        val miss = cache.get(accountId) as FirstPageLookup.Miss
        val stale = counter("statement.cache.writes", "stale")
        val ok = counter("statement.cache.writes", "ok")

        cache.evict(accountId)
        cache.put(accountId, StatementPage(emptyList(), null), miss.version)

        assertThat(redisTemplate.hasKey(key(accountId))).isFalse()
        assertThat(counter("statement.cache.writes", "stale")).isEqualTo(stale + 1)
        assertThat(counter("statement.cache.writes", "ok")).isEqualTo(ok)
    }

    @Test
    fun `the key is statement first-page v1 and its ttl is between 50 and 70 seconds`() {
        val accountId = UUID.randomUUID()

        cache.put(accountId, StatementPage(emptyList(), null), 0)

        val ttl = redisTemplate.getExpire(key(accountId))
        assertThat(ttl).isBetween(50L, 70L)
    }

    @Test
    fun `evict removes the key`() {
        val accountId = UUID.randomUUID()
        cache.put(accountId, StatementPage(emptyList(), null), 0)
        val before = counter("statement.cache.evictions", "ok")

        cache.evict(accountId)

        assertThat(redisTemplate.hasKey(key(accountId))).isFalse()
        assertThat(counter("statement.cache.evictions", "ok")).isEqualTo(before + 1)
    }

    @Test
    fun `get counts hit and miss`() {
        val accountId = UUID.randomUUID()
        val misses = counter("statement.cache.requests", "miss")
        val hits = counter("statement.cache.requests", "hit")
        val writes = counter("statement.cache.writes", "ok")

        assertThat(cache.get(accountId)).isEqualTo(FirstPageLookup.Miss(0))
        cache.put(accountId, StatementPage(emptyList(), null), 0)
        assertThat(cache.get(accountId)).isInstanceOf(FirstPageLookup.Hit::class.java)

        assertThat(counter("statement.cache.requests", "miss")).isEqualTo(misses + 1)
        assertThat(counter("statement.cache.requests", "hit")).isEqualTo(hits + 1)
        assertThat(counter("statement.cache.writes", "ok")).isEqualTo(writes + 1)
    }

    @Test
    fun `an invalid json value is a miss`() {
        val accountId = UUID.randomUUID()
        redisTemplate.opsForValue().set(key(accountId), "{not json")
        val misses = counter("statement.cache.requests", "miss")
        val errors = counter("statement.cache.requests", "error")

        assertThat(cache.get(accountId)).isEqualTo(FirstPageLookup.Miss(0))

        assertThat(counter("statement.cache.requests", "miss")).isEqualTo(misses + 1)
        assertThat(counter("statement.cache.requests", "error")).isEqualTo(errors)
    }

    @Test
    fun `with redis paused, get is unavailable within 1 second and counts an error`() {
        val accountId = UUID.randomUUID()
        val page = StatementPage(emptyList(), null)
        // Warm the connection, so the paused read exercises the command timeout and not the first connect.
        cache.put(accountId, page, 0)
        val requestErrors = counter("statement.cache.requests", "error")
        val writeErrors = counter("statement.cache.writes", "error")
        val evictionErrors = counter("statement.cache.evictions", "error")

        RedisTestSupport.pause(redis)
        try {
            var result: FirstPageLookup? = null
            val elapsed = measureTimeMillis { result = cache.get(accountId) }
            cache.put(accountId, page, 0)
            cache.evict(accountId)

            assertThat(result).isEqualTo(FirstPageLookup.Unavailable)
            assertThat(elapsed).isLessThan(1_000)
        } finally {
            RedisTestSupport.unpause(redis)
        }

        // Reads and writes are counted apart: the get is a request error, the put a write error.
        assertThat(counter("statement.cache.requests", "error")).isEqualTo(requestErrors + 1)
        assertThat(counter("statement.cache.writes", "error")).isEqualTo(writeErrors + 1)
        assertThat(counter("statement.cache.evictions", "error")).isEqualTo(evictionErrors + 1)
    }
}
