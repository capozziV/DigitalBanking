package contracts.http

import sh.stubborn.contract.spec.Contract

Contract.make {
    description 'transfers finds the entry recorded with a key (GET /accounts/{id}/entries?idempotencyKey=): 200 with the entry'
    request {
        method GET()
        urlPath('/accounts/0198a1b2-0000-7000-8000-000000000001/entries') {
            queryParameters {
                parameter('idempotencyKey', 'transfer-1-debit')
            }
        }
        headers {
            header('Authorization', $(consumer(regex('Bearer .+')), producer(execute('serviceAuthorization()'))))
            header('X-Correlation-Id', $(consumer(regex('.+')), producer('contract-correlation-id')))
        }
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
            kind: 'DEBIT',
            amount: 1000,
            balanceAfter: 0,
            description: 'Transfer to someone',
            createdAt: '2026-10-27T10:00:00Z',
        )
    }
}
