package contracts.http

import sh.stubborn.contract.spec.Contract

// A customer who never opened an account, or whose accounts are all closed, gets 404.
Contract.make {
    description 'BFF reads the own account of a customer without an open account: 404 account-not-found'
    request {
        method GET()
        url '/accounts/me'
        headers {
            header('Authorization', $(consumer(regex('Bearer .+')), producer(execute('customerWithoutAccountAuthorization()'))))
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
            detail: 'customer 0198a1b2-0000-7000-8000-0000000000c2 has no open account',
        )
    }
}
