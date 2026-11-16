package io.github.capozziv.digitalbanking.notification.adapters.web

import io.github.capozziv.digitalbanking.security.CallerRoles.ADMIN
import io.github.capozziv.digitalbanking.security.CallerRoles.CUSTOMER
import io.github.capozziv.digitalbanking.security.SecurityRules
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod

/**
 * Route rules of the notification API. Every route is listed: the shared filter chain denies what is not declared.
 * Whose notifications are read or marked is decided by the use cases, from the `Caller`.
 */
@Configuration
class NotificationSecurityConfiguration {
    @Bean
    fun notificationSecurityRules() =
        SecurityRules {
            authorize(HttpMethod.GET, "/notifications", hasAnyRole(CUSTOMER, ADMIN))
            authorize(HttpMethod.POST, "/notifications/read", hasAnyRole(CUSTOMER, ADMIN))
        }
}
