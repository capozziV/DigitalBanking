package io.github.capozziv.digitalbanking.transfers.application.port

import io.github.capozziv.digitalbanking.transfers.domain.RecordedEntry
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

/** Answer of `accounts` to a read of one ledger entry by its idempotency key. */
sealed interface EntryLookup {
    data class Found(
        val entry: RecordedEntry,
    ) : EntryLookup

    /** No entry under the key on that account (or no such account). */
    data object NotFound : EntryLookup

    /** `accounts` did not answer in time, or answered with an error. */
    data object Unavailable : EntryLookup
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

    /** The entry recorded on [accountId] under [idempotencyKey] (`GET /accounts/{id}/entries?idempotencyKey=`). */
    fun findEntry(
        accountId: UUID,
        idempotencyKey: String,
    ): EntryLookup

    fun perform(call: StepCall.Accounts): CallResult
}
