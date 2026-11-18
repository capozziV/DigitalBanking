package contracts.http

import sh.stubborn.contract.spec.Contract

Contract.make {
    description 'a debit above the available balance: 422 insufficient-available-balance with the available balance'
    request {
        method POST()
        url '/accounts/0198a1b2-0000-7000-8000-000000000001/debits'
        headers {
            header('Authorization', $(consumer(regex('Bearer .+')), producer(execute('serviceAuthorization()'))))
            header('X-Correlation-Id', $(consumer(regex('.+')), producer('contract-correlation-id')))
            header('Idempotency-Key', $(consumer(regex('.+')), producer('transfer-2-debit')))
            contentType(applicationJson())
        }
        body(
            amount: 5000,
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
            type: '/problems/insufficient-available-balance',
            title: 'Insufficient available balance',
            status: 422,
            detail: 'The available balance does not cover the debit',
            availableBalance: 1000,
        )
    }
}
