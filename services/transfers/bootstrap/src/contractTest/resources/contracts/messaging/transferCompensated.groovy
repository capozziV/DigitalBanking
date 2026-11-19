package contracts.messaging

import sh.stubborn.contract.spec.Contract

// TransferCompensated v1, as notification tells the sender the amount came back.
Contract.make {
    description 'transfers returns the amount of a transfer to the source'
    label 'transferCompensated'
    input {
        triggeredBy('transferCompensated()')
    }
    outputMessage {
        sentTo 'transfers-events.fifo'
        body(
            eventId: $(consumer('0198a1b2-0000-7000-8000-0000000e2003'), producer(regex(uuid()))),
            eventType: 'TransferCompensated',
            eventVersion: 1,
            occurredAt: $(consumer('2026-11-18T12:00:00.000001Z'), producer(regex(iso8601WithOffset()))),
            aggregateId: '0198a1b2-0000-7000-8000-00000000f001',
            correlationId: 'contract-correlation-id',
            payload: [
                transferId          : '0198a1b2-0000-7000-8000-00000000f001',
                sourceAccountId     : '0198a1b2-0000-7000-8000-000000000001',
                destinationAccountId: '0198a1b2-0000-7000-8000-000000000002',
                amount              : 5000,
                description         : 'Rent',
            ],
        )
    }
}
