package io.github.capozziv.digitalbanking.accounts.application.fake

import io.github.capozziv.digitalbanking.accounts.application.port.EventOutbox
import io.github.capozziv.digitalbanking.accounts.domain.AccountEvent

class RecordingEventOutbox : EventOutbox {
    val events = mutableListOf<AccountEvent>()

    override fun append(event: AccountEvent) {
        events += event
    }
}
