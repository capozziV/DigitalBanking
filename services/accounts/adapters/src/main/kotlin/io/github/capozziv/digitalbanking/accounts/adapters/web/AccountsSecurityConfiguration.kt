package io.github.capozziv.digitalbanking.accounts.adapters.web

import io.github.capozziv.digitalbanking.security.CallerRoles.ADMIN
import io.github.capozziv.digitalbanking.security.CallerRoles.CUSTOMER
import io.github.capozziv.digitalbanking.security.CallerRoles.SERVICE
import io.github.capozziv.digitalbanking.security.SecurityRules
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod

/**
 * Which kind of caller may reach each route of [AccountController]. libs/security denies any route without a rule,
 * so every route is listed here; the routes under `/admin/` (account types) are admin only by a rule of the library.
 *
 * The rules only check the kind of caller. Whether a customer owns the account is checked by the use cases, which
 * answer 404 for the account of another customer.
 */
@Configuration
class AccountsSecurityConfiguration {
    @Bean
    fun accountsSecurityRules() =
        SecurityRules {
            authorize(HttpMethod.POST, "/accounts", hasAnyRole(CUSTOMER, ADMIN))
            authorize(HttpMethod.GET, "/accounts/{id}", hasAnyRole(CUSTOMER, ADMIN, SERVICE))
            authorize(HttpMethod.GET, "/accounts/{id}/entries", hasAnyRole(CUSTOMER, ADMIN, SERVICE))
            authorize(HttpMethod.POST, "/accounts/{id}/credits", hasAnyRole(SERVICE, ADMIN))
            authorize(HttpMethod.POST, "/accounts/{id}/debits", hasAnyRole(SERVICE, ADMIN))
            authorize(HttpMethod.PUT, "/accounts/{id}/type", hasRole(ADMIN))
        }
}
