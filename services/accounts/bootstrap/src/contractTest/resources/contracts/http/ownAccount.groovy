package contracts.http

import sh.stubborn.contract.spec.Contract

// GET /accounts/me as the BFF calls it: the account of the customer of the token (no account id known).
Contract.make {
    description 'BFF reads the own account of the logged customer'
    request {
        method GET()
        url '/accounts/me'
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
            id: '0198a1b2-0000-7000-8000-000000000001',
            customerId: '0198a1b2-0000-7000-8000-0000000000c1',
            type: 'FREE',
            status: 'ACTIVE',
            balance: 1000,
            overdraftLimit: 0,
            availableBalance: 1000,
        )
    }
}
