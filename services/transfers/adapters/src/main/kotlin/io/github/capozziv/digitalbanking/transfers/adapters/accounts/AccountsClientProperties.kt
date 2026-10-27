package io.github.capozziv.digitalbanking.transfers.adapters.accounts

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/** Settings of the HTTP client that calls `accounts` (`transfers.accounts.*`). */
@ConfigurationProperties("transfers.accounts")
data class AccountsClientProperties(
    val url: String = "http://localhost:8081",
    val connectTimeout: Duration = Duration.ofSeconds(1),
    val readTimeout: Duration = Duration.ofSeconds(3),
)
