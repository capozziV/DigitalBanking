package io.github.capozziv.digitalbanking.transfers.application.port

import io.github.capozziv.digitalbanking.transfers.domain.StepCall
import io.github.capozziv.digitalbanking.transfers.domain.StepOutcome
import java.util.UUID

/** Answer of `accounts` to a lookup of one account. */
sealed interface AccountLookup {
    /** The account exists and belongs to [customerId]. */
    data class Found(
        val customerId: UUID,
    ) : AccountLookup

    data object NotFound : AccountLookup

    /** `accounts` did not answer in time, or answered with an error. */
    data object Unavailable : AccountLookup
}

/** Classified result of a call to `accounts`; [httpStatus] is null when no response arrived (timeout, cut connection). */
data class CallResult(
    val outcome: StepOutcome,
    val httpStatus: Int?,
    val detail: String?,
)

/** Calls to the `accounts` service. Must never be invoked inside a database transaction. */
interface AccountsGateway {
    fun lookupAccount(id: UUID): AccountLookup

    fun perform(call: StepCall.Accounts): CallResult
}
