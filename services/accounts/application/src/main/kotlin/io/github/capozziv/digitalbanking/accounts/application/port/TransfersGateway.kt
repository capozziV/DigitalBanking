package io.github.capozziv.digitalbanking.accounts.application.port

import java.util.UUID

/**
 * The `transfers` service, asked before closing an account (no cache: it is the source of truth of the transfers).
 * Fails with `TransfersUnavailableException` when the service is down, slow or answers an error.
 */
interface TransfersGateway {
    /** How many transfers leaving [accountId] are still PENDING. */
    fun pendingOutgoing(accountId: UUID): Int
}
