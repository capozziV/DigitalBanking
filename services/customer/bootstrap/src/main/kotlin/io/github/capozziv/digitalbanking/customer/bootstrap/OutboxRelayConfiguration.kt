package io.github.capozziv.digitalbanking.customer.bootstrap

import io.github.capozziv.digitalbanking.outbox.relay.spring.OutboxRelayConfigurationSupport
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.Configuration

/**
 * Publishes the outbox to the SNS FIFO topic of the service. Disabled with `customer.outbox.relay-enabled=false`
 * (the integration tests that do not exercise the relay). Properties under `customer.outbox.*` and `customer.aws.*`.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty(name = ["customer.outbox.relay-enabled"], matchIfMissing = true)
class OutboxRelayConfiguration : OutboxRelayConfigurationSupport(propertyPrefix = "customer", lockKey = RELAY_LOCK_KEY) {
    companion object {
        /**
         * Advisory lock that elects the single active relay of the customer service: the ASCII bytes of
         * "customer". No other advisory lock is taken on the customer database.
         */
        const val RELAY_LOCK_KEY = 0x637573746F6D6572L
    }
}
