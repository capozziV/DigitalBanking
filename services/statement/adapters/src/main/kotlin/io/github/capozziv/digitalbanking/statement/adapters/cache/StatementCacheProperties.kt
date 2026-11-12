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
)
