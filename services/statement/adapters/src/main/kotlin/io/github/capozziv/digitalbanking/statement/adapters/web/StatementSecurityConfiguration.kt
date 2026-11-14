package io.github.capozziv.digitalbanking.statement.adapters.web

import io.github.capozziv.digitalbanking.security.CallerRoles.ADMIN
import io.github.capozziv.digitalbanking.security.CallerRoles.CUSTOMER
import io.github.capozziv.digitalbanking.security.SecurityRules
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod

/**
 * Route rules of the statement API. Every route is listed: the shared filter chain denies what is not declared.
 * The rules only say which kind of caller reaches the route; the owner of the account is checked by the use case.
 */
@Configuration
class StatementSecurityConfiguration {
    @Bean
    fun statementSecurityRules() =
        SecurityRules {
            authorize(HttpMethod.GET, "/accounts/{accountId}/statement", hasAnyRole(CUSTOMER, ADMIN))
        }
}
