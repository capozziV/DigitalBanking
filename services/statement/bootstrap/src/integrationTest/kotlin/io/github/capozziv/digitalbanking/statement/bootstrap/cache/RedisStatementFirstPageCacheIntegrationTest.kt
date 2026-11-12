package io.github.capozziv.digitalbanking.statement.bootstrap.cache

import io.github.capozziv.digitalbanking.statement.adapters.cache.RedisStatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.bootstrap.StatementIntegrationTest
import io.github.capozziv.digitalbanking.statement.domain.EntryKind
import io.github.capozziv.digitalbanking.statement.domain.StatementCursor
import io.github.capozziv.digitalbanking.statement.domain.StatementEntry
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
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

        cache.put(accountId, page)
        cache.put(emptyAccountId, empty)

        assertThat(cache.get(accountId)).isEqualTo(page)
        assertThat(cache.get(emptyAccountId)).isEqualTo(empty)
    }

    @Test
    fun `the key is statement first-page v1 and its ttl is between 50 and 70 seconds`() {
        val accountId = UUID.randomUUID()

        cache.put(accountId, StatementPage(emptyList(), null))

        val ttl = redisTemplate.getExpire(key(accountId))
        assertThat(ttl).isBetween(50L, 70L)
    }

    @Test
    fun `evict removes the key`() {
        val accountId = UUID.randomUUID()
        cache.put(accountId, StatementPage(emptyList(), null))
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

        assertThat(cache.get(accountId)).isNull()
        cache.put(accountId, StatementPage(emptyList(), null))
        assertThat(cache.get(accountId)).isNotNull()

        assertThat(counter("statement.cache.requests", "miss")).isEqualTo(misses + 1)
        assertThat(counter("statement.cache.requests", "hit")).isEqualTo(hits + 1)
    }

    @Test
    fun `an invalid json value is a miss`() {
        val accountId = UUID.randomUUID()
        redisTemplate.opsForValue().set(key(accountId), "{not json")
        val misses = counter("statement.cache.requests", "miss")
        val errors = counter("statement.cache.requests", "error")

        assertThat(cache.get(accountId)).isNull()

        assertThat(counter("statement.cache.requests", "miss")).isEqualTo(misses + 1)
        assertThat(counter("statement.cache.requests", "error")).isEqualTo(errors)
    }

    @Test
    fun `with redis paused, get returns null within 1 second and counts an error`() {
        val accountId = UUID.randomUUID()
        val page = StatementPage(emptyList(), null)
        // Warm the connection, so the paused read exercises the command timeout and not the first connect.
        cache.put(accountId, page)
        val requestErrors = counter("statement.cache.requests", "error")
        val evictionErrors = counter("statement.cache.evictions", "error")

        redis.dockerClient.pauseContainerCmd(redis.containerId).exec()
        try {
            var result: StatementPage? = page
            val elapsed = measureTimeMillis { result = cache.get(accountId) }
            cache.put(accountId, page)
            cache.evict(accountId)

            assertThat(result).isNull()
            assertThat(elapsed).isLessThan(1_000)
        } finally {
            redis.dockerClient.unpauseContainerCmd(redis.containerId).exec()
        }

        // One for the get and one for the put.
        assertThat(counter("statement.cache.requests", "error")).isEqualTo(requestErrors + 2)
        assertThat(counter("statement.cache.evictions", "error")).isEqualTo(evictionErrors + 1)
    }
}
