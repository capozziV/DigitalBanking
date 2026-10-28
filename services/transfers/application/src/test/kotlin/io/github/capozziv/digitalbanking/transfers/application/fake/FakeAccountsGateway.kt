package io.github.capozziv.digitalbanking.transfers.application.fake

import io.github.capozziv.digitalbanking.transfers.application.port.AccountLookup
import io.github.capozziv.digitalbanking.transfers.application.port.AccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.port.CallResult
import io.github.capozziv.digitalbanking.transfers.domain.AccountsCall
import io.github.capozziv.digitalbanking.transfers.domain.StepOutcome
import java.util.UUID

/** Replies with a script: one [CallResult] per call, in order. A call beyond the script fails the test. */
class FakeAccountsGateway(
    private val transactions: ImmediateTransactionRunner? = null,
) : AccountsGateway {
    private val script = ArrayDeque<CallResult>()
    val calls = mutableListOf<AccountsCall>()
    var lookup = AccountLookup.EXISTS

    /** Per-account override of [lookup]. */
    val lookupResults = mutableMapOf<UUID, AccountLookup>()
    val lookups = mutableListOf<UUID>()

    /** Runs on every [perform] after the call is recorded, e.g. to advance a test clock by the call's duration. */
    var onCall: (AccountsCall) -> Unit = {}

    fun reply(vararg results: CallResult) {
        script.addAll(results)
    }

    override fun lookupAccount(id: UUID): AccountLookup {
        lookups += id
        return lookupResults[id] ?: lookup
    }

    override fun perform(call: AccountsCall): CallResult {
        check(transactions?.open != true) { "accounts called inside a database transaction" }
        calls += call
        onCall(call)
        return script.removeFirstOrNull() ?: error("no scripted reply for call #${calls.size}: $call")
    }

    companion object {
        val created = CallResult(StepOutcome.Success, 201, null)
        val busy = CallResult(StepOutcome.TransientFailure, 503, "account-busy")
        val timeout = CallResult(StepOutcome.TransientFailure, null, "read timed out")

        fun refused(reason: String) = CallResult(StepOutcome.Refused(reason), 422, reason)
    }
}
