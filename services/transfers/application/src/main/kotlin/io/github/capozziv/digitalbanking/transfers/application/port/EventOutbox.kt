package io.github.capozziv.digitalbanking.transfers.application.port

import io.github.capozziv.digitalbanking.transfers.domain.TransferEvent

/** Writes the event to the outbox in the caller's transaction. */
interface EventOutbox {
    fun append(event: TransferEvent)
}
