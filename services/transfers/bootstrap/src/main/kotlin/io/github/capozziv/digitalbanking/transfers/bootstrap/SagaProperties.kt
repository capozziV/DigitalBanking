package io.github.capozziv.digitalbanking.transfers.bootstrap

import io.github.capozziv.digitalbanking.transfers.application.usecase.RetryBackoff
import io.github.capozziv.digitalbanking.transfers.application.usecase.SagaSettings
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/** Settings of the transfer saga (`transfers.saga.*`); the defaults are those of the spec. */
@ConfigurationProperties("transfers.saga")
data class SagaProperties(
    val deadline: Duration = Duration.ofMinutes(2),
    val lease: Duration = Duration.ofSeconds(30),
    val busyAttempts: Int = 3,
    val callBudget: Duration = Duration.ofSeconds(4),
    val dueBatchSize: Int = 50,
    val retryInitial: Duration = Duration.ofSeconds(1),
    val retryMax: Duration = Duration.ofSeconds(15),
) {
    fun toSettings() =
        SagaSettings(
            deadline = deadline,
            lease = lease,
            busyAttempts = busyAttempts,
            dueBatchSize = dueBatchSize,
            callBudget = callBudget,
        )

    fun toBackoff() = RetryBackoff(retryInitial, retryMax)
}
