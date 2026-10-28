package io.github.capozziv.digitalbanking.transfers.application.port

import io.github.capozziv.digitalbanking.transfers.domain.AccountsCall
import io.github.capozziv.digitalbanking.transfers.domain.StepOutcome
import java.util.UUID

enum class AccountLookup { EXISTS, NOT_FOUND, UNAVAILABLE }

/** Classified result of a call to `accounts`; [httpStatus] is null when no response arrived (timeout, cut connection). */
data class CallResult(
    val outcome: StepOutcome,
    val httpStatus: Int?,
    val detail: String?,
)

/** Calls to the `accounts` service. Must never be invoked inside a database transaction. */
interface AccountsGateway {
    fun lookupAccount(id: UUID): AccountLookup

    fun perform(call: AccountsCall): CallResult
}
