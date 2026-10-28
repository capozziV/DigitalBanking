package io.github.capozziv.digitalbanking.transfers.bootstrap

import io.github.capozziv.digitalbanking.transfers.adapters.accounts.AccountsClientProperties
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component

/**
 * Checks at startup the rule that spans two property groups: the saga only starts a call to `accounts` while at least
 * `transfers.saga.call-budget` of the lease is left, so the budget must cover the worst case of the call, the connect
 * plus the read timeout of the client (`transfers.accounts.*`). Otherwise a call could outlast the lease it relies on.
 */
@Component
class SagaTimeoutsValidator(
    private val saga: SagaProperties,
    private val accounts: AccountsClientProperties,
) : InitializingBean {
    override fun afterPropertiesSet() {
        val worstCase = accounts.connectTimeout + accounts.readTimeout
        check(saga.callBudget >= worstCase) {
            "transfers.saga.call-budget (${saga.callBudget}) must be at least " +
                "transfers.accounts.connect-timeout (${accounts.connectTimeout}) + " +
                "transfers.accounts.read-timeout (${accounts.readTimeout}) = $worstCase"
        }
    }
}
