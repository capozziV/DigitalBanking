package io.github.capozziv.digitalbanking.statement.adapters.cache

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * First-page cache settings (`statement.cache.*`). Each page lives for [ttl] plus or minus a random up-to-[ttlJitter],
 * so pages written together do not all expire together.
 */
@ConfigurationProperties("statement.cache")
data class StatementCacheProperties(
    val enabled: Boolean = true,
    val ttl: Duration = Duration.ofSeconds(60),
    val ttlJitter: Duration = Duration.ofSeconds(10),
) {
    init {
        require(!ttl.isNegative && !ttl.isZero) { "statement.cache.ttl must be positive, was $ttl" }
        require(!ttlJitter.isNegative) { "statement.cache.ttl-jitter must not be negative, was $ttlJitter" }
        require(ttl > ttlJitter) { "statement.cache.ttl-jitter ($ttlJitter) must be shorter than statement.cache.ttl ($ttl)" }
    }
}
