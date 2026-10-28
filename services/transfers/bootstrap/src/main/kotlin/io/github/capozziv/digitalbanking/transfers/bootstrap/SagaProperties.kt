package io.github.capozziv.digitalbanking.transfers.bootstrap

import io.github.capozziv.digitalbanking.transfers.application.RetryBackoff
import io.github.capozziv.digitalbanking.transfers.application.usecase.SagaSettings
import jakarta.validation.constraints.Positive
import org.hibernate.validator.constraints.time.DurationMin
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

/**
 * Settings of the transfer saga (`transfers.saga.*`); the defaults are those of the spec. Validated at startup:
 * counts and durations must be positive. [pollInterval] is read by [TransferSagaScheduler]; [schedulerEnabled] turns
 * the scheduler off (integration tests that do not exercise it).
 */
@ConfigurationProperties("transfers.saga")
@Validated
data class SagaProperties(
    @field:DurationMin(nanos = 1)
    val deadline: Duration = Duration.ofMinutes(2),
    @field:DurationMin(nanos = 1)
    val lease: Duration = Duration.ofSeconds(30),
    @field:Positive
    val busyAttempts: Int = 3,
    @field:DurationMin(nanos = 1)
    val callBudget: Duration = Duration.ofSeconds(4),
    @field:Positive
    val dueBatchSize: Int = 50,
    @field:DurationMin(nanos = 1)
    val retryInitial: Duration = Duration.ofSeconds(1),
    @field:DurationMin(nanos = 1)
    val retryMax: Duration = Duration.ofSeconds(15),
    @field:DurationMin(nanos = 1)
    val pollInterval: Duration = Duration.ofSeconds(1),
    val schedulerEnabled: Boolean = true,
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
