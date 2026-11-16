package io.github.capozziv.digitalbanking.customer.adapters.web

import io.github.capozziv.digitalbanking.security.CallerRoles.ADMIN
import io.github.capozziv.digitalbanking.security.CallerRoles.CUSTOMER
import io.github.capozziv.digitalbanking.security.SecurityRules
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod

/**
 * Route rules of the customer service. The routes under `/admin` (admin) and `/internal` (service) are fixed by the
 * security library; every other route has to be listed here, or it is denied. Whose customer it is never needs
 * checking: the customer routes act on the id of the caller.
 */
@Configuration(proxyBeanMethods = false)
class CustomerSecurityConfiguration {
    @Bean
    fun customerSecurityRules() =
        SecurityRules {
            authorize(HttpMethod.POST, "/customers", hasAnyRole(CUSTOMER, ADMIN))
            authorize(HttpMethod.GET, "/customers/me", hasAnyRole(CUSTOMER, ADMIN))
        }
}
