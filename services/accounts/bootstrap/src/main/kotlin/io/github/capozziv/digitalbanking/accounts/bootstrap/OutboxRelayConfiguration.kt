package io.github.capozziv.digitalbanking.accounts.bootstrap

import io.github.capozziv.digitalbanking.outbox.relay.spring.OutboxRelayConfigurationSupport
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.Configuration

/**
 * Publishes the outbox to the SNS FIFO topic of the service. Disabled with `accounts.outbox.relay-enabled=false`
 * (the integration tests that do not exercise the relay). Properties under `accounts.outbox.*` and `accounts.aws.*`.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty(name = ["accounts.outbox.relay-enabled"], matchIfMissing = true)
class OutboxRelayConfiguration : OutboxRelayConfigurationSupport(propertyPrefix = "accounts", lockKey = RELAY_LOCK_KEY) {
    companion object {
        /**
         * Advisory lock that elects the single active relay of the accounts service: the ASCII bytes of
         * "accounts". No other advisory lock is taken on the accounts database.
         */
        const val RELAY_LOCK_KEY = 0x6163636F756E7473L
    }
}
