package io.github.capozziv.digitalbanking.accounts.application.port

import io.github.capozziv.digitalbanking.accounts.domain.AccountEvent

interface EventOutbox {
    /**
     * Writes the event to the outbox table in the caller's transaction (never publishes directly), so the
     * event commits or rolls back together with the change that produced it.
     */
    fun append(event: AccountEvent)
}
