package contracts.messaging

import sh.stubborn.contract.spec.Contract

// LedgerEntryRecorded v1 of a credit with a description, as statement projects it into the statement entry.
Contract.make {
    description 'accounts records a credit with a description'
    label 'creditRecorded'
    input {
        triggeredBy('creditRecorded()')
    }
    outputMessage {
        sentTo 'accounts-events.fifo'
        body(
            eventId: $(consumer('0198a1b2-0000-7000-8000-0000000e0002'), producer(regex(uuid()))),
            eventType: 'LedgerEntryRecorded',
            eventVersion: 1,
            occurredAt: $(consumer('2026-11-18T12:00:00.000001Z'), producer(regex(iso8601WithOffset()))),
            aggregateId: '0198a1b2-0000-7000-8000-000000000001',
            correlationId: 'contract-correlation-id',
            payload: [
                entryId       : '0198a1b2-0000-7000-8000-0000000e1002',
                accountId     : '0198a1b2-0000-7000-8000-000000000001',
                sequence      : 2,
                kind          : 'CREDIT',
                amount        : 5000,
                balanceAfter  : 6000,
                description   : 'Transfer received',
                idempotencyKey: 'transfer-0198a1b2-0000-7000-8000-00000000f001-credit',
                createdAt     : '2026-11-18T12:00:00Z',
            ],
        )
    }
}
