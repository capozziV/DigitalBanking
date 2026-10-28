package io.github.capozziv.digitalbanking.outbox.relay

import io.github.capozziv.digitalbanking.outbox.PendingEvent

/** Publishes one outbox event to the message broker. Throws when the broker does not accept it. */
interface EventPublisher {
    fun publish(event: PendingEvent)
}
