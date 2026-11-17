package io.github.capozziv.digitalbanking.accounts.adapters.transfers

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/** Settings of the HTTP client that calls the `transfers` service (`accounts.transfers.*`). */
@ConfigurationProperties("accounts.transfers")
data class TransfersClientProperties(
    val baseUrl: String = "http://localhost:8082",
    /**
     * Connect and read timeout of each call (500 ms by default): a slower `transfers` is answered with 503 and the
     * account is not closed. The machine token is fetched before the call, outside this timeout, and is cached.
     */
    val timeout: Duration = Duration.ofMillis(500),
)
