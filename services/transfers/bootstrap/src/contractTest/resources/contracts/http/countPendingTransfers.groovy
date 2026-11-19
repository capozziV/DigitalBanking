package contracts.http

import sh.stubborn.contract.spec.Contract

Contract.make {
    description 'accounts counts the PENDING transfers leaving an account before closing it (GET /internal/accounts/{id}/pending-transfers): 200 with the count'
    request {
        method GET()
        url '/internal/accounts/0198a1b2-0000-7000-8000-0000000000d1/pending-transfers'
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
            count: 3,
        )
    }
}
