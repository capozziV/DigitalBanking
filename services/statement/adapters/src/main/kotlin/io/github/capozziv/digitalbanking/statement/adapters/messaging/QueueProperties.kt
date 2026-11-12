package io.github.capozziv.digitalbanking.statement.adapters.messaging

import org.springframework.boot.context.properties.ConfigurationProperties

/** Names of the queue this service consumes and of its DLQ (`statement.queues.*`). */
@ConfigurationProperties("statement.queues")
data class QueueProperties(
    val accounts: String,
    val accountsDlq: String,
) {
    val deadLetterQueues: List<String> get() = listOf(accountsDlq)
}
