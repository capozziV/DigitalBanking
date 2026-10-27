package io.github.capozziv.digitalbanking.transfers.application.usecase

import java.time.Duration

/**
 * [busyAttempts] is the total number of calls (the first one included) a step makes in a row on `503 account-busy`.
 * [callBudget] is the worst-case duration of one call to `accounts` (connect 1 s + read 3 s): a call only starts while
 * at least that much of the lease is left.
 */
data class SagaSettings(
    val deadline: Duration = Duration.ofMinutes(2),
    val lease: Duration = Duration.ofSeconds(30),
    val busyAttempts: Int = 3,
    val dueBatchSize: Int = 50,
    val callBudget: Duration = Duration.ofSeconds(4),
)
