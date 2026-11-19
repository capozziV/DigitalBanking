package contracts.messaging

import sh.stubborn.contract.spec.Contract

// AccountOpened v1 as statement (registers the owner) and notification (welcome) read it from accounts-events.fifo.
Contract.make {
    description 'accounts opens an account'
    label 'accountOpened'
    input {
        triggeredBy('accountOpened()')
    }
    outputMessage {
        sentTo 'accounts-events.fifo'
        body(
            eventId: $(consumer('0198a1b2-0000-7000-8000-0000000e0001'), producer(regex(uuid()))),
            eventType: 'AccountOpened',
            eventVersion: 1,
            occurredAt: $(consumer('2026-11-18T12:00:00Z'), producer(regex(iso8601WithOffset()))),
            aggregateId: '0198a1b2-0000-7000-8000-000000000001',
            correlationId: 'contract-correlation-id',
            payload: [
                accountId : '0198a1b2-0000-7000-8000-000000000001',
                customerId: '0198a1b2-0000-7000-8000-0000000000c1',
                type      : 'FREE',
            ],
        )
    }
}
