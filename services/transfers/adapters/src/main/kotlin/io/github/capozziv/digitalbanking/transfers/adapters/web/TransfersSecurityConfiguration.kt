package io.github.capozziv.digitalbanking.transfers.adapters.web

import io.github.capozziv.digitalbanking.security.CallerRoles.ADMIN
import io.github.capozziv.digitalbanking.security.CallerRoles.CUSTOMER
import io.github.capozziv.digitalbanking.security.SecurityRules
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod

/**
 * Who may reach each route of transfers. Every route of [TransferController] and [DailyLimitController] is listed:
 * the security library denies any route without a rule. Service tokens get none of them (no peer calls transfers).
 * Whether the transfer or the account belongs to the caller is decided by the use cases (404 for someone else's).
 */
@Configuration
class TransfersSecurityConfiguration {
    @Bean
    fun transfersSecurityRules() =
        SecurityRules {
            authorize(HttpMethod.POST, "/transfers", hasAnyRole(CUSTOMER, ADMIN))
            authorize(HttpMethod.GET, "/transfers/{id}", hasAnyRole(CUSTOMER, ADMIN))
            authorize(HttpMethod.GET, "/accounts/{accountId}/daily-limit", hasAnyRole(CUSTOMER, ADMIN))
            // The YAML form of the OpenAPI contract: the library opens only /v3/api-docs/**, which does not match it.
            authorize(HttpMethod.GET, "/v3/api-docs.yaml", permitAll)
        }
}
