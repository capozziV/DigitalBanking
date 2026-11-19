package contracts.messaging

import sh.stubborn.contract.spec.Contract

// CustomerRegistered v2 (v1 plus the phone, E.164) from customer-events.fifo. No service consumes it yet; the contract
// pins the payload for the first consumer. A reader of v1 ignores the extra field.
Contract.make {
    description 'customer registers a customer'
    label 'customerRegistered'
    input {
        triggeredBy('customerRegistered()')
    }
    outputMessage {
        sentTo 'customer-events.fifo'
        body(
            eventId: $(consumer('0198a1b2-0000-7000-8000-0000000e0011'), producer(regex(uuid()))),
            eventType: 'CustomerRegistered',
            eventVersion: 2,
            occurredAt: $(consumer('2026-11-19T12:00:00Z'), producer(regex(iso8601WithOffset()))),
            aggregateId: '0198a1b2-0000-7000-8000-0000000000c1',
            correlationId: 'contract-correlation-id',
            payload: [
                customerId: '0198a1b2-0000-7000-8000-0000000000c1',
                name      : 'Ana Souza',
                email     : 'ana@example.test',
                phone     : '+5511987654321',
            ],
        )
    }
}
