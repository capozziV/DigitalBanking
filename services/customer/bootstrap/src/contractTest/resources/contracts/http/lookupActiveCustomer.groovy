package contracts.http

import sh.stubborn.contract.spec.Contract

Contract.make {
    description 'accounts and transfers look up an active customer (GET /internal/customers/{id}): 200 ACTIVE'
    request {
        method GET()
        url '/internal/customers/0198a1b2-0000-7000-8000-0000000000c1'
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
            id: '0198a1b2-0000-7000-8000-0000000000c1',
            status: 'ACTIVE',
        )
    }
}
