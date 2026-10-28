package io.github.capozziv.digitalbanking.transfers.bootstrap

import io.github.capozziv.digitalbanking.outbox.relay.spring.OutboxRelayConfigurationSupport
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.Configuration

/**
 * Publishes the outbox to the SNS FIFO topic of the service (`transfers-events.fifo`), the same way as the accounts
 * relay. Disabled with `transfers.outbox.relay-enabled=false` (the integration tests that do not exercise the relay).
 * Properties under `transfers.outbox.*` and `transfers.aws.*`.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty(name = ["transfers.outbox.relay-enabled"], matchIfMissing = true)
class OutboxRelayConfiguration : OutboxRelayConfigurationSupport(propertyPrefix = "transfers", lockKey = RELAY_LOCK_KEY) {
    companion object {
        /**
         * Advisory lock that elects the single active relay of the transfers service: the ASCII bytes of "transfer".
         * The transfers database is its own, but the key differs from the accounts one by convention (spec of stage 2, section 6).
         * No other advisory lock is taken on the transfers database.
         */
        const val RELAY_LOCK_KEY = 0x7472616E73666572L
    }
}
