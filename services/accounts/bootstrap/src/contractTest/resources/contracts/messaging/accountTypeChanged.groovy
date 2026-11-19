package contracts.messaging

import sh.stubborn.contract.spec.Contract

// AccountTypeChanged v2 (with the overdraft limit of the new type, in cents), as notification tells the customer.
Contract.make {
    description 'accounts changes the type of an account'
    label 'accountTypeChanged'
    input {
        triggeredBy('accountTypeChanged()')
    }
    outputMessage {
        sentTo 'accounts-events.fifo'
        body(
            eventId: $(consumer('0198a1b2-0000-7000-8000-0000000e0004'), producer(regex(uuid()))),
            eventType: 'AccountTypeChanged',
            eventVersion: 2,
            occurredAt: $(consumer('2026-11-18T12:10:00.000001Z'), producer(regex(iso8601WithOffset()))),
            aggregateId: '0198a1b2-0000-7000-8000-000000000001',
            correlationId: 'contract-correlation-id',
            payload: [
                accountId     : '0198a1b2-0000-7000-8000-000000000001',
                previousType  : 'FREE',
                newType       : 'GOLD',
                overdraftLimit: 200000,
            ],
        )
    }
}
