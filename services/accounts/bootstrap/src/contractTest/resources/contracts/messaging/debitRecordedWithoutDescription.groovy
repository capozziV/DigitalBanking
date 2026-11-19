package contracts.messaging

import sh.stubborn.contract.spec.Contract

// LedgerEntryRecorded v1 of a debit without a description (null, not absent) and with a fractional createdAt, as
// statement projects it; the balance after it is negative (overdraft).
Contract.make {
    description 'accounts records a debit without a description'
    label 'debitRecordedWithoutDescription'
    input {
        triggeredBy('debitRecordedWithoutDescription()')
    }
    outputMessage {
        sentTo 'accounts-events.fifo'
        body(
            eventId: $(consumer('0198a1b2-0000-7000-8000-0000000e0003'), producer(regex(uuid()))),
            eventType: 'LedgerEntryRecorded',
            eventVersion: 1,
            occurredAt: $(consumer('2026-11-18T12:05:00.000001Z'), producer(regex(iso8601WithOffset()))),
            aggregateId: '0198a1b2-0000-7000-8000-000000000001',
            correlationId: 'contract-correlation-id',
            payload: [
                entryId       : '0198a1b2-0000-7000-8000-0000000e1003',
                accountId     : '0198a1b2-0000-7000-8000-000000000001',
                sequence      : 3,
                kind          : 'DEBIT',
                amount        : 1500,
                balanceAfter  : -500,
                description   : null,
                idempotencyKey: 'debit-key-3',
                createdAt     : '2026-11-18T12:05:00.123456Z',
            ],
        )
    }
}
