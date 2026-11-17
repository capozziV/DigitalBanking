package io.github.capozziv.digitalbanking.accounts.application.fake

import io.github.capozziv.digitalbanking.accounts.application.TransfersUnavailableException
import io.github.capozziv.digitalbanking.accounts.application.port.TransfersGateway
import java.util.UUID

/** Transfers service stand-in: no pending transfer unless set; records the lookups and can fail or run a hook. */
class StubTransfersGateway : TransfersGateway {
    private val pending = mutableMapOf<UUID, Int>()
    val lookups = mutableListOf<UUID>()
    var unavailable = false

    /** Runs during the lookup, as if something happened while the `transfers` service was being asked. */
    var duringLookup: () -> Unit = {}

    fun set(
        accountId: UUID,
        count: Int,
    ) {
        pending[accountId] = count
    }

    override fun pendingOutgoing(accountId: UUID): Int {
        lookups += accountId
        duringLookup()
        if (unavailable) throw TransfersUnavailableException("transfers service down")
        return pending[accountId] ?: 0
    }
}
