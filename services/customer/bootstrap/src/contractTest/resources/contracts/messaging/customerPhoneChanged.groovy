package contracts.messaging

import sh.stubborn.contract.spec.Contract

// CustomerPhoneChanged v1 from customer-events.fifo: the phone before and after the change, both E.164. No service
// consumes it yet; the contract pins the payload for the first consumer.
Contract.make {
    description 'customer changes the phone of a customer'
    label 'customerPhoneChanged'
    input {
        triggeredBy('customerPhoneChanged()')
    }
    outputMessage {
        sentTo 'customer-events.fifo'
        body(
            eventId: $(consumer('0198a1b2-0000-7000-8000-0000000e0012'), producer(regex(uuid()))),
            eventType: 'CustomerPhoneChanged',
            eventVersion: 1,
            occurredAt: $(consumer('2026-11-19T12:05:00Z'), producer(regex(iso8601WithOffset()))),
            aggregateId: '0198a1b2-0000-7000-8000-0000000000c1',
            correlationId: 'contract-correlation-id',
            payload: [
                customerId   : '0198a1b2-0000-7000-8000-0000000000c1',
                previousPhone: '+5511987654321',
                newPhone     : '+5521988776655',
            ],
        )
    }
}
