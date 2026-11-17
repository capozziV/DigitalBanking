package io.github.capozziv.digitalbanking.transfers.application.fake

import io.github.capozziv.digitalbanking.transfers.application.port.AccountLookup
import io.github.capozziv.digitalbanking.transfers.application.port.AccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.port.CallResult
import io.github.capozziv.digitalbanking.transfers.application.port.EntryLookup
import io.github.capozziv.digitalbanking.transfers.domain.RecordedEntry
import io.github.capozziv.digitalbanking.transfers.domain.StepCall
import io.github.capozziv.digitalbanking.transfers.domain.StepOutcome
import java.util.UUID

/** Replies with a script: one [CallResult] per call, in order. A call beyond the script fails the test. */
class FakeAccountsGateway(
    private val transactions: ImmediateTransactionRunner? = null,
) : AccountsGateway {
    private val script = ArrayDeque<CallResult>()
    val calls = mutableListOf<StepCall.Accounts>()
    var lookup: AccountLookup = AccountLookup.Found(OWNER)

    /** Per-account override of [lookup]. */
    val lookupResults = mutableMapOf<UUID, AccountLookup>()
    val lookups = mutableListOf<UUID>()

    /** Entries of `accounts` by account and idempotency key, as `GET /accounts/{id}/entries?idempotencyKey=` reads them. */
    val entries = mutableMapOf<Pair<UUID, String>, RecordedEntry>()

    /** When true, [findEntry] answers [EntryLookup.Unavailable]. */
    var entriesUnavailable = false
    val entryLookups = mutableListOf<Pair<UUID, String>>()

    /** Runs on every [perform] after the call is recorded, e.g. to advance a test clock by the call's duration. */
    var onCall: (StepCall.Accounts) -> Unit = {}

    fun reply(vararg results: CallResult) {
        script.addAll(results)
    }

    override fun lookupAccount(id: UUID): AccountLookup {
        lookups += id
        return lookupResults[id] ?: lookup
    }

    override fun findEntry(
        accountId: UUID,
        idempotencyKey: String,
    ): EntryLookup {
        check(transactions?.open != true) { "accounts called inside a database transaction" }
        entryLookups += accountId to idempotencyKey
        if (entriesUnavailable) return EntryLookup.Unavailable
        return entries[accountId to idempotencyKey]?.let { EntryLookup.Found(it) } ?: EntryLookup.NotFound
    }

    override fun perform(call: StepCall.Accounts): CallResult {
        check(transactions?.open != true) { "accounts called inside a database transaction" }
        calls += call
        onCall(call)
        return script.removeFirstOrNull() ?: error("no scripted reply for call #${calls.size}: $call")
    }

    companion object {
        /** Customer that owns every account unless [lookupResults] says otherwise. */
        val OWNER: UUID = UUID.fromString("00000000-0000-7000-8000-0000000000c3")

        val created = CallResult(StepOutcome.Success, 201, null)
        val busy = CallResult(StepOutcome.TransientFailure, 503, "account-busy")
        val timeout = CallResult(StepOutcome.TransientFailure, null, "read timed out")

        fun refused(reason: String) = CallResult(StepOutcome.Refused(reason), 422, reason)
    }
}
