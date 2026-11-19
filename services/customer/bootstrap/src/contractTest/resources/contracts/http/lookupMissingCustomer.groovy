package contracts.http

import sh.stubborn.contract.spec.Contract

Contract.make {
    description 'a customer that does not exist: 404 customer-not-found'
    request {
        method GET()
        url '/internal/customers/0198a1b2-0000-7000-8000-000000000404'
        headers {
            header('Authorization', $(consumer(regex('Bearer .+')), producer(execute('serviceAuthorization()'))))
            header('X-Correlation-Id', $(consumer(regex('.+')), producer('contract-correlation-id')))
        }
    }
    response {
        status NOT_FOUND()
        headers {
            contentType('application/problem+json')
            header('X-Correlation-Id', $(consumer('contract-correlation-id'), producer('contract-correlation-id')))
        }
        body(
            type: '/problems/customer-not-found',
            title: 'Customer not found',
            status: 404,
            detail: 'customer 0198a1b2-0000-7000-8000-000000000404 not found',
        )
    }
}
