package io.github.capozziv.digitalbanking.accounts.application.fake

import io.github.capozziv.digitalbanking.accounts.application.port.EventOutbox
import io.github.capozziv.digitalbanking.accounts.domain.AccountEvent

class RecordingEventOutbox : EventOutbox {
    private val recorded = mutableListOf<AccountEvent>()

    val events: List<AccountEvent> get() = recorded.toList()

    override fun append(event: AccountEvent) {
        recorded += event
    }
}
