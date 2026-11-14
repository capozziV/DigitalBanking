package io.github.capozziv.digitalbanking.transfers.bootstrap

import io.github.capozziv.digitalbanking.security.ServiceTokenClient
import io.github.capozziv.digitalbanking.transfers.adapters.accounts.AccountsClientProperties
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Checks at startup the rule that spans three property groups: the saga only starts a call to `accounts` while at
 * least `transfers.saga.call-budget` of the lease is left, so the budget must cover the worst case of the call. That is
 * the connect plus the read timeout of the client (`transfers.accounts.*`), plus the worst case of getting the machine
 * token ([ServiceTokenClient.maxFetchDuration], `digitalbanking.security.service-token.*`), which the interceptor may
 * spend before the request leaves. Otherwise a call could outlast the lease it relies on.
 */
@Component
class SagaTimeoutsValidator(
    private val saga: SagaProperties,
    private val accounts: AccountsClientProperties,
    private val serviceToken: ObjectProvider<ServiceTokenClient>,
) : InitializingBean {
    override fun afterPropertiesSet() {
        val token = serviceToken.ifAvailable?.maxFetchDuration ?: Duration.ZERO
        val worstCase = accounts.connectTimeout + accounts.readTimeout + token
        check(saga.callBudget >= worstCase) {
            "transfers.saga.call-budget (${saga.callBudget}) must be at least " +
                "transfers.accounts.connect-timeout (${accounts.connectTimeout}) + " +
                "transfers.accounts.read-timeout (${accounts.readTimeout}) + " +
                "the worst case of the service token ($token) = $worstCase"
        }
    }
}
