package contracts.messaging

import sh.stubborn.contract.spec.Contract

// AccountUnblocked v1 (accountId, customerId, occurredAt), as notification tells the customer.
Contract.make {
    description 'accounts '
    label 'accountUnblocked'
    input {
        triggeredBy('accountUnblocked()')
    }
    outputMessage {
        sentTo 'accounts-events.fifo'
        body(
            eventId: $(consumer('0198a1b2-0000-7000-8000-0000000e0006'), producer(regex(uuid()))),
            eventType: 'AccountUnblocked',
            eventVersion: 1,
            occurredAt: $(consumer('2026-11-18T12:20:00.000001Z'), producer(regex(iso8601WithOffset()))),
            aggregateId: '0198a1b2-0000-7000-8000-000000000001',
            correlationId: 'contract-correlation-id',
            payload: [
                accountId  : '0198a1b2-0000-7000-8000-000000000001',
                customerId : '0198a1b2-0000-7000-8000-0000000000c1',
                occurredAt : '2026-11-18T12:20:00Z',
            ],
        )
    }
}
