package io.github.capozziv.digitalbanking.transfers.domain

/** Result of the call a [TransferStep] makes to `accounts`, already classified. */
sealed interface StepOutcome {
    data object Success : StepOutcome

    data class Refused(
        val reason: String,
    ) : StepOutcome

    data object TransientFailure : StepOutcome

    companion object {
        const val INSUFFICIENT_AVAILABLE_BALANCE = "insufficient-available-balance"
        const val ENTRY_NOT_FOUND = "entry-not-found"
    }
}
