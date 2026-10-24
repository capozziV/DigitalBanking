package io.github.capozziv.digitalbanking.outbox

/** Publishes one outbox event to the message broker. Throws when the broker does not accept it. */
interface EventPublisher {
    fun publish(event: PendingEvent)
}
