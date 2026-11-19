package contracts.http

import sh.stubborn.contract.spec.Contract

Contract.make {
    description 'a key already used for another operation: 422 idempotency-key-reused'
    request {
        method POST()
        url '/accounts/0198a1b2-0000-7000-8000-000000000001/debits'
        headers {
            header('Authorization', $(consumer(regex('Bearer .+')), producer(execute('serviceAuthorization()'))))
            header('X-Correlation-Id', $(consumer(regex('.+')), producer('contract-correlation-id')))
            header('Idempotency-Key', $(consumer('transfer-5-debit'), producer('transfer-5-debit')))
            contentType(applicationJson())
        }
        body(
            amount: 1000,
            description: 'Transfer to someone',
        )
    }
    response {
        status 422
        headers {
            contentType('application/problem+json')
            header('X-Correlation-Id', $(consumer('contract-correlation-id'), producer('contract-correlation-id')))
        }
        body(
            type: '/problems/idempotency-key-reused',
            title: 'Idempotency key reused',
            status: 422,
            detail: 'The Idempotency-Key was already used for another operation',
        )
    }
}
