package contracts.http

import sh.stubborn.contract.spec.Contract

Contract.make {
    description 'no entry was recorded with the key: 404 entry-not-found'
    request {
        method GET()
        urlPath('/accounts/0198a1b2-0000-7000-8000-000000000001/entries') {
            queryParameters {
                parameter('idempotencyKey', 'never-used')
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
            type: '/problems/entry-not-found',
            title: 'Entry not found',
            status: 404,
            detail: 'No entry with the given idempotency key on this account',
        )
    }
}
