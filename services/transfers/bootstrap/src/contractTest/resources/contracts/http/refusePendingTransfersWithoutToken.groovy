package contracts.http

import sh.stubborn.contract.spec.Contract

Contract.make {
    description 'a call to GET /internal/accounts/{id}/pending-transfers without a token is refused: 401'
    request {
        method GET()
        url '/internal/accounts/0198a1b2-0000-7000-8000-0000000000d3/pending-transfers'
    }
    response {
        status UNAUTHORIZED()
    }
}
