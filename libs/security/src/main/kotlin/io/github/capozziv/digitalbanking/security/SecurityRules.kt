package io.github.capozziv.digitalbanking.security

import org.springframework.security.config.annotation.web.AuthorizeHttpRequestsDsl

/**
 * The route rules of one service, plugged into the security filter chain of [DigitalBankingSecurityAutoConfiguration].
 *
 * Declare it as a bean in the service (more than one bean is fine; they apply in bean order):
 * ```
 * @Bean
 * fun accountsSecurityRules() =
 *     SecurityRules {
 *         authorize(HttpMethod.POST, "/accounts", hasAnyRole(CallerRoles.CUSTOMER, CallerRoles.ADMIN))
 *         authorize(HttpMethod.POST, "/accounts/{id}/debits", hasAnyRole(CallerRoles.SERVICE, CallerRoles.ADMIN))
 *     }
 * ```
 * The block is the Spring Security Kotlin DSL of `authorizeHttpRequests`. The chain wraps it like this (first match
 * wins):
 * 1. fixed rules: `/actuator/health/` and the OpenAPI docs open, `/error` open, `/admin/` for [CallerRoles.ADMIN],
 *    `/internal/` for [CallerRoles.SERVICE], the rest of `/actuator/` for [CallerRoles.ADMIN];
 * 2. the rules of this service;
 * 3. anything else is denied (403 with a token, 401 without). A route nobody declared is never open by accident.
 *
 * The rules only say which kind of caller may reach a route. Whether the resource belongs to the caller is the use
 * case's job, with [Caller.actsAsCustomer] (and 404 for someone else's resource).
 */
class SecurityRules(
    private val rules: AuthorizeHttpRequestsDsl.() -> Unit,
) {
    internal fun applyTo(dsl: AuthorizeHttpRequestsDsl) = dsl.rules()
}

/** The role each kind of [Caller] gets, for `hasRole`/`hasAnyRole` in [SecurityRules]. */
object CallerRoles {
    const val CUSTOMER = "CUSTOMER"
    const val ADMIN = "ADMIN"
    const val SERVICE = "SERVICE"

    internal fun of(caller: Caller): String =
        when (caller) {
            is Caller.Customer -> CUSTOMER
            is Caller.Admin -> ADMIN
            Caller.Service -> SERVICE
        }
}
