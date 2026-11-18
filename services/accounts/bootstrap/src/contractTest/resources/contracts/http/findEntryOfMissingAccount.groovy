package contracts.http

import sh.stubborn.contract.spec.Contract

Contract.make {
    description 'the entry of an account that does not exist: 404 account-not-found'
    request {
        method GET()
        urlPath('/accounts/0198a1b2-0000-7000-8000-000000000404/entries') {
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
        status 404
        headers {
            contentType('application/problem+json')
            header('X-Correlation-Id', $(consumer('contract-correlation-id'), producer('contract-correlation-id')))
        }
        body(
            type: '/problems/account-not-found',
            title: 'Account not found',
            status: 404,
            detail: 'account 0198a1b2-0000-7000-8000-000000000404 not found',
        )
    }
}
