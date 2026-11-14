package io.github.capozziv.digitalbanking.transfers.adapters.customer

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Settings of the HTTP client that calls `customer` (`transfers.customer.*`). [timeout] bounds the connection and,
 * separately, the whole answer: a slow `customer` makes the request 503, never a long wait.
 */
@ConfigurationProperties("transfers.customer")
data class CustomerClientProperties(
    val url: String = "http://localhost:8085",
    val timeout: Duration = Duration.ofMillis(500),
)
