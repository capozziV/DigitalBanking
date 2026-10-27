package io.github.capozziv.digitalbanking.transfers.application.usecase

import java.time.Duration

data class SagaSettings(
    val deadline: Duration = Duration.ofMinutes(2),
    val lease: Duration = Duration.ofSeconds(30),
    val busyRetries: Int = 3,
    val dueBatchSize: Int = 50,
)
