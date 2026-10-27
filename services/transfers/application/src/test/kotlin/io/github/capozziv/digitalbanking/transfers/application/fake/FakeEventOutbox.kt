package io.github.capozziv.digitalbanking.transfers.application.fake

import io.github.capozziv.digitalbanking.transfers.application.port.EventOutbox
import io.github.capozziv.digitalbanking.transfers.domain.TransferEvent

class FakeEventOutbox : EventOutbox {
    val events = mutableListOf<TransferEvent>()

    override fun append(event: TransferEvent) {
        events += event
    }
}
