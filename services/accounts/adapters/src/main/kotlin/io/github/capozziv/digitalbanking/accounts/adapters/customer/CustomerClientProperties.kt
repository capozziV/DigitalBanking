package io.github.capozziv.digitalbanking.accounts.adapters.customer

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/** Settings of the HTTP client that calls the `customer` service (`accounts.customer.*`). */
@ConfigurationProperties("accounts.customer")
data class CustomerClientProperties(
    val baseUrl: String = "http://localhost:8085",
    /**
     * Connect and read timeout of each call (500 ms by default): a slower `customer` is answered with 503. The
     * machine token is fetched before the call, outside this timeout, and is cached for minutes.
     */
    val timeout: Duration = Duration.ofMillis(500),
)
