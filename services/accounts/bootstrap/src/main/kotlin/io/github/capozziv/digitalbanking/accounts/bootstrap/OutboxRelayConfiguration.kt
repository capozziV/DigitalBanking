package io.github.capozziv.digitalbanking.accounts.bootstrap

import io.github.capozziv.digitalbanking.outbox.EventPublisher
import io.github.capozziv.digitalbanking.outbox.JdbcOutboxStore
import io.github.capozziv.digitalbanking.outbox.OutboxMetrics
import io.github.capozziv.digitalbanking.outbox.OutboxRelay
import io.github.capozziv.digitalbanking.outbox.OutboxRelaySettings
import io.github.capozziv.digitalbanking.outbox.OutboxStore
import io.github.capozziv.digitalbanking.outbox.SnsEventPublisher
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.support.TransactionTemplate
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import java.net.URI
import java.time.Clock
import java.time.Duration

/**
 * Publishes the outbox to the SNS FIFO topic of the service. Disabled with `accounts.outbox.relay-enabled=false`
 * (the integration tests that do not exercise the relay).
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(OutboxRelayProperties::class, AwsProperties::class)
@ConditionalOnBooleanProperty(name = ["accounts.outbox.relay-enabled"], matchIfMissing = true)
class OutboxRelayConfiguration {
    @Bean
    fun outboxRelaySettings(properties: OutboxRelayProperties) =
        OutboxRelaySettings(
            batchSize = properties.batchSize,
            alertAfterAttempts = properties.alertAfterAttempts,
            lockKey = RELAY_LOCK_KEY,
            publishTimeout = properties.publishTimeout,
            maxCycleDuration = properties.maxCycleDuration,
        )

    /**
     * With an endpoint (LocalStack), static test credentials; without one, the AWS default credential chain.
     * A bean of its own because the SNS client does not close a provider it was given: the default chain is
     * AutoCloseable, so Spring closes it on shutdown (inferred destroy method).
     */
    @Bean
    fun awsCredentialsProvider(aws: AwsProperties): AwsCredentialsProvider =
        if (aws.endpoint.isNullOrBlank()) {
            DefaultCredentialsProvider.builder().build()
        } else {
            StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"))
        }

    /** The endpoint override only with an endpoint (LocalStack). Client timeouts back up the per-request publish timeout. */
    @Bean(destroyMethod = "close")
    fun snsClient(
        aws: AwsProperties,
        credentials: AwsCredentialsProvider,
        settings: OutboxRelaySettings,
    ): SnsClient {
        val builder =
            SnsClient
                .builder()
                .region(Region.of(aws.region))
                .credentialsProvider(credentials)
                .overrideConfiguration {
                    it.apiCallTimeout(settings.publishTimeout).apiCallAttemptTimeout(settings.publishTimeout)
                }
        if (!aws.endpoint.isNullOrBlank()) builder.endpointOverride(URI.create(aws.endpoint))
        return builder.build()
    }

    @Bean
    fun snsEventPublisher(
        snsClient: SnsClient,
        aws: AwsProperties,
        settings: OutboxRelaySettings,
    ) = SnsEventPublisher(snsClient, aws.topicArn, settings.publishTimeout)

    @Bean
    fun outboxStore(
        jdbcClient: JdbcClient,
        transactionTemplate: TransactionTemplate,
    ): OutboxStore = JdbcOutboxStore(jdbcClient, transactionTemplate)

    @Bean
    fun outboxMetrics(registry: MeterRegistry) = OutboxMetrics(registry)

    @Bean
    fun outboxRelay(
        store: OutboxStore,
        publisher: EventPublisher,
        settings: OutboxRelaySettings,
        clock: Clock,
        metrics: OutboxMetrics,
    ) = OutboxRelay(store, publisher, settings, clock, metrics)

    /**
     * Waits on shutdown for the longest a cycle can take: its maximum duration plus one publish call.
     * Depends on `taskScheduler` (the bean name of Spring Boot's scheduler, with platform or virtual threads)
     * so that it is destroyed first: closing that scheduler calls `shutdownNow()` on the executor running the cycle.
     */
    @Bean
    @DependsOn("taskScheduler")
    fun outboxRelayScheduler(
        relay: OutboxRelay,
        settings: OutboxRelaySettings,
    ) = OutboxRelayScheduler(relay, settings.maxCycleDuration + settings.publishTimeout + SHUTDOWN_MARGIN)

    companion object {
        /**
         * Advisory lock that elects the single active relay of the accounts service: the ASCII bytes of
         * "accounts". No other advisory lock is taken on the accounts database.
         */
        const val RELAY_LOCK_KEY = 0x6163636F756E7473L
        private val SHUTDOWN_MARGIN: Duration = Duration.ofSeconds(1)
    }
}

/** `accounts.outbox.*`: relay tuning (see [OutboxRelaySettings]); the poll interval is read by [OutboxRelayScheduler]. */
@ConfigurationProperties("accounts.outbox")
data class OutboxRelayProperties(
    val batchSize: Int = 100,
    val alertAfterAttempts: Int = 10,
    val publishTimeout: Duration = Duration.ofSeconds(2),
    val maxCycleDuration: Duration = Duration.ofSeconds(10),
)

/** `accounts.aws.*`: where the events go. A blank [endpoint] means the real AWS endpoint of [region]. */
@ConfigurationProperties("accounts.aws")
data class AwsProperties(
    val endpoint: String? = null,
    val region: String,
    val topicArn: String,
)
