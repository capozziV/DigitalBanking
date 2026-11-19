package contracts.http

import sh.stubborn.contract.spec.Contract

Contract.make {
    description 'transfers debits the source account (POST /accounts/{id}/debits): 201 with the ledger entry'
    request {
        method POST()
        url '/accounts/0198a1b2-0000-7000-8000-000000000001/debits'
        headers {
            header('Authorization', $(consumer(regex('Bearer .+')), producer(execute('serviceAuthorization()'))))
            header('X-Correlation-Id', $(consumer(regex('.+')), producer('contract-correlation-id')))
            header('Idempotency-Key', $(consumer('transfer-1-debit'), producer('transfer-1-debit')))
            contentType(applicationJson())
        }
        body(
            amount: 1000,
            description: 'Transfer to someone',
        )
    }
    response {
        status CREATED()
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
