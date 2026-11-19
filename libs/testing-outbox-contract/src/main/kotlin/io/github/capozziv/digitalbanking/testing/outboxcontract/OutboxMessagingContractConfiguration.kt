package io.github.capozziv.digitalbanking.testing.outboxcontract

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.simple.JdbcClient
import sh.stubborn.contract.verifier.messaging.ContractMessage
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierObjectMapper
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import javax.sql.DataSource

/**
 * What the generated message contract tests of a producer need, besides the service's own outbox mapper and writer
 * (which the base class imports): the outbox database, a clock, the [OutboxMessageVerifier] and the two beans the
 * generated tests autowire.
 *
 * The base class sets `digitalbanking.contract.messaging.topic-arn` to the topic of the service, for example
 * `${'$'}{accounts.aws.topic-arn}`. Stubborn's own fallback (NoOpContractVerifierAutoConfiguration) builds its
 * ContractVerifierMessaging from no-op receivers, so it is replaced here, not just given a receiver.
 */
@Configuration(proxyBeanMethods = false)
class OutboxMessagingContractConfiguration {
    @Bean
    fun outboxContractDataSource(): DataSource = OutboxContractDatabase.dataSource

    @Bean
    fun outboxContractJdbcClient(dataSource: DataSource): JdbcClient = JdbcClient.create(dataSource)

    @Bean
    fun outboxContractClock(): Clock = Clock.systemUTC()

    @Bean
    fun outboxMessageVerifier(
        jdbcClient: JdbcClient,
        @Value("\${digitalbanking.contract.messaging.topic-arn}") topicArn: String,
    ): OutboxMessageVerifier = OutboxMessageVerifier(jdbcClient, topicArn)

    @Bean
    fun contractVerifierMessaging(verifier: OutboxMessageVerifier): ContractVerifierMessaging<ContractMessage> =
        ContractVerifierMessaging(verifier, verifier)

    @Bean
    fun contractVerifierObjectMapper(jsonMapper: ObjectProvider<JsonMapper>): ContractVerifierObjectMapper =
        ContractVerifierObjectMapper(jsonMapper.getIfAvailable { JsonMapper.builder().build() })
}
