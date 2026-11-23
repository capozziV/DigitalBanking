package io.github.capozziv.digitalbanking.statement.adapters.cache

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.github.capozziv.digitalbanking.observability.CorrelationId
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.time.Duration
import java.util.UUID
import kotlin.random.Random

class RedisStatementFirstPageCacheTest {
    private val properties =
        StatementCacheProperties(enabled = true, ttl = Duration.ofSeconds(60), ttlJitter = Duration.ofSeconds(10))

    // The template is never used by nextTtl, so it needs no connection factory.
    private fun cache(
        properties: StatementCacheProperties,
        random: Random = Random.Default,
        template: StringRedisTemplate = StringRedisTemplate(),
    ) = RedisStatementFirstPageCache(
        template,
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

    private val logger = LoggerFactory.getLogger(RedisStatementFirstPageCache::class.java) as Logger
    private val logged = ListAppender<ILoggingEvent>()

    @BeforeEach
    fun captureLogs() {
        logger.level = Level.INFO
        logged.start()
        logger.addAppender(logged)
        MDC.put(CorrelationId.MDC_KEY, "corr-cache")
    }

    @AfterEach
    fun releaseLogs() {
        logger.detachAppender(logged)
        MDC.clear()
    }

    /** A template whose every Redis call fails, as with the server down: its factory cannot open a connection. */
    private fun failingTemplate(): StringRedisTemplate =
        StringRedisTemplate(
            java.lang.reflect.Proxy.newProxyInstance(
                javaClass.classLoader,
                arrayOf(RedisConnectionFactory::class.java),
            ) { _, _, _ -> throw IllegalStateException("redis down") } as RedisConnectionFactory,
        )

    private fun warnings() = logged.list.filter { it.level == Level.WARN }

    @Test
    fun `the WARN of a failed get, put and evict carries the correlation ID of the MDC`() {
        val cache = cache(properties, template = failingTemplate())
        val accountId = UUID.randomUUID()

        cache.get(accountId)
        cache.put(accountId, StatementPage(emptyList(), null), 0)
        cache.evict(accountId)

        assertThat(warnings()).hasSize(3)
        assertThat(warnings()).allSatisfy { assertThat(it.mdcPropertyMap[CorrelationId.MDC_KEY]).isEqualTo("corr-cache") }
    }

    @Test
    fun `the WARN of an unreadable cached value carries the correlation ID of the MDC`() {
        // The MGET answers a page that is not JSON and no version.
        val values =
            java.lang.reflect.Proxy.newProxyInstance(
                javaClass.classLoader,
                arrayOf(ValueOperations::class.java),
            ) { _, _, _ -> listOf("{not json", null) }
        val template =
            object : StringRedisTemplate() {
                @Suppress("UNCHECKED_CAST")
                override fun opsForValue(): ValueOperations<String, String> = values as ValueOperations<String, String>
            }

        cache(properties, template = template).get(UUID.randomUUID())

        assertThat(warnings().single().mdcPropertyMap[CorrelationId.MDC_KEY]).isEqualTo("corr-cache")
    }
}
