package io.github.capozziv.digitalbanking.notification.adapters.messaging

import org.springframework.boot.context.properties.ConfigurationProperties

/** Names of the queues this service consumes and of their DLQs (`notification.queues.*`). */
@ConfigurationProperties("notification.queues")
data class QueueProperties(
    val accounts: String,
    val transfers: String,
    val customer: String,
    val accountsDlq: String,
    val transfersDlq: String,
    val customerDlq: String,
) {
    val deadLetterQueues: List<String> get() = listOf(accountsDlq, transfersDlq, customerDlq)
}
