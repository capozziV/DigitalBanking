package io.github.capozziv.digitalbanking.transfers.domain

/** Result of the call a [TransferStep] makes (to `accounts` or to the daily limit), already classified. */
sealed interface StepOutcome {
    data object Success : StepOutcome

    data class Refused(
        val reason: String,
    ) : StepOutcome

    data object TransientFailure : StepOutcome

    companion object {
        const val INSUFFICIENT_AVAILABLE_BALANCE = "insufficient-available-balance"
        const val ENTRY_NOT_FOUND = "entry-not-found"

        /** Failure reason exposed when the debit could not be confirmed (not the internal [ENTRY_NOT_FOUND]). */
        const val DEBIT_NOT_CONFIRMED = "debit-not-confirmed"

        /** The transfer would take the source account over its daily limit. */
        const val DAILY_LIMIT_EXCEEDED = "daily-limit-exceeded"

        /** The daily limit could not be checked before the saga deadline. */
        const val DAILY_LIMIT_UNAVAILABLE = "daily-limit-unavailable"
    }
}
