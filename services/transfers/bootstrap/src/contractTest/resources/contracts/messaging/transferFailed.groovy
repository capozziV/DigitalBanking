package contracts.messaging

import sh.stubborn.contract.spec.Contract

// TransferFailed v1 (no description, with the failure reason), as notification tells the sender it was refused.
Contract.make {
    description 'transfers refuses a transfer before taking the amount from the source'
    label 'transferFailed'
    input {
        triggeredBy('transferFailed()')
    }
    outputMessage {
        sentTo 'transfers-events.fifo'
        body(
            eventId: $(consumer('0198a1b2-0000-7000-8000-0000000e2002'), producer(regex(uuid()))),
            eventType: 'TransferFailed',
            eventVersion: 1,
            occurredAt: $(consumer('2026-11-18T12:00:00.000001Z'), producer(regex(iso8601WithOffset()))),
            aggregateId: '0198a1b2-0000-7000-8000-00000000f001',
            correlationId: 'contract-correlation-id',
            payload: [
                transferId          : '0198a1b2-0000-7000-8000-00000000f001',
                sourceAccountId     : '0198a1b2-0000-7000-8000-000000000001',
                destinationAccountId: '0198a1b2-0000-7000-8000-000000000002',
                amount              : 5000,
                description         : null,
                reason              : 'insufficient-available-balance',
            ],
        )
    }
}
