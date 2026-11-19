package contracts.http

import sh.stubborn.contract.spec.Contract

// GET /accounts/recipient as the BFF calls it: only the account id, no balance and no type.
Contract.make {
    description 'BFF finds the account that receives a transfer to a customer'
    request {
        method GET()
        url '/accounts/recipient?customerId=0198a1b2-0000-7000-8000-0000000000c1'
        headers {
            header('Authorization', $(consumer(regex('Bearer .+')), producer(execute('customerAuthorization()'))))
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
            accountId: '0198a1b2-0000-7000-8000-000000000001',
        )
    }
}
