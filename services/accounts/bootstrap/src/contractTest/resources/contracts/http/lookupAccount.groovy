package contracts.http

import sh.stubborn.contract.spec.Contract

// GET /accounts/{id} as transfers calls it (HttpAccountsGateway.lookupAccount): the owner of an existing account.
Contract.make {
    description 'transfers looks up an account to learn its owner'
    request {
        method GET()
        url '/accounts/0198a1b2-0000-7000-8000-000000000001'
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
            id: '0198a1b2-0000-7000-8000-000000000001',
            customerId: '0198a1b2-0000-7000-8000-0000000000c1',
            type: 'FREE',
            balance: 1000,
            overdraftLimit: 0,
            availableBalance: 1000,
        )
    }
}
