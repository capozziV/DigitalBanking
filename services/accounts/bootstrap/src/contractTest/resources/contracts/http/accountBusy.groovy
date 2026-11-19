package contracts.http

import sh.stubborn.contract.spec.Contract

Contract.make {
    description 'an account whose lock is held: 503 account-busy with Retry-After'
    request {
        method POST()
        url '/accounts/0198a1b2-0000-7000-8000-000000000503/debits'
        headers {
            header('Authorization', $(consumer(regex('Bearer .+')), producer(execute('serviceAuthorization()'))))
            header('X-Correlation-Id', $(consumer(regex('.+')), producer('contract-correlation-id')))
            header('Idempotency-Key', $(consumer('transfer-6-debit'), producer('transfer-6-debit')))
            contentType(applicationJson())
        }
        body(
            amount: 1000,
            description: 'Transfer to someone',
        )
    }
    response {
        status 503
        headers {
            contentType('application/problem+json')
            header('Retry-After', '1')
            header('X-Correlation-Id', $(consumer('contract-correlation-id'), producer('contract-correlation-id')))
        }
        body(
            type: '/problems/account-busy',
            title: 'Account busy',
            status: 503,
            detail: 'The account is busy; retry shortly',
        )
    }
}
