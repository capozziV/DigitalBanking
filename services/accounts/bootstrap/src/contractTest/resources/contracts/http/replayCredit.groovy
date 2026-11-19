package contracts.http

import sh.stubborn.contract.spec.Contract

Contract.make {
    description 'a credit repeated with a key already recorded (POST /accounts/{id}/credits): 200 with the same entry'
    request {
        method POST()
        url '/accounts/0198a1b2-0000-7000-8000-000000000001/credits'
        headers {
            header('Authorization', $(consumer(regex('Bearer .+')), producer(execute('serviceAuthorization()'))))
            header('X-Correlation-Id', $(consumer(regex('.+')), producer('contract-correlation-id')))
            header('Idempotency-Key', $(consumer('transfer-9-credit'), producer('transfer-9-credit')))
            contentType(applicationJson())
        }
        body(
            amount: 1000,
            description: 'Transfer from someone',
        )
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
            header('X-Correlation-Id', $(consumer('contract-correlation-id'), producer('contract-correlation-id')))
        }
        body(
            id: '0198a1b2-0000-7000-8000-0000000000aa',
            accountId: '0198a1b2-0000-7000-8000-000000000001',
            sequence: 2,
            kind: 'CREDIT',
            amount: 1000,
            balanceAfter: 2000,
            description: 'Transfer from someone',
            createdAt: '2026-10-27T10:00:00Z',
        )
    }
}
