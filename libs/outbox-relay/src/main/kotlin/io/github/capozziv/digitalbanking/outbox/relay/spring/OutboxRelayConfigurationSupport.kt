package io.github.capozziv.digitalbanking.outbox.relay.spring

import io.github.capozziv.digitalbanking.outbox.relay.EventPublisher
import io.github.capozziv.digitalbanking.outbox.relay.JdbcOutboxStore
import io.github.capozziv.digitalbanking.outbox.relay.OutboxMetrics
import io.github.capozziv.digitalbanking.outbox.relay.OutboxRelay
import io.github.capozziv.digitalbanking.outbox.relay.OutboxRelaySettings
import io.github.capozziv.digitalbanking.outbox.relay.OutboxStore
import io.github.capozziv.digitalbanking.outbox.relay.SnsEventPublisher
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.core.env.Environment
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
 * Publishes the outbox of a service to its SNS FIFO topic. A service extends this class in a `@Configuration`
 * of its own, passing the property prefix (`accounts`, `transfers`...) and the advisory lock key that elects its
 * single active relay, and keeps the `@ConditionalOnBooleanProperty("<prefix>.outbox.relay-enabled")` switch on it.
 *
 * Reads `<prefix>.outbox.*` ([OutboxRelayProperties]) and `<prefix>.aws.*` ([AwsProperties]) through the [Binder],
 * since a property prefix cannot be a parameter of `@ConfigurationProperties`.
 */
@EnableScheduling
abstract class OutboxRelayConfigurationSupport(
    private val propertyPrefix: String,
    private val lockKey: Long,
) {
    @Bean
    fun outboxRelayProperties(environment: Environment): OutboxRelayProperties =
        Binder
            .get(environment)
            .bind("$propertyPrefix.outbox", Bindable.of(OutboxRelayProperties::class.java))
            .orElseGet { OutboxRelayProperties() }

    @Bean
    fun awsProperties(environment: Environment): AwsProperties =
        Binder
            .get(environment)
            .bind("$propertyPrefix.aws", Bindable.of(AwsProperties::class.java))
            .get()

    @Bean
    fun outboxRelaySettings(properties: OutboxRelayProperties) =
        OutboxRelaySettings(
            batchSize = properties.batchSize,
            alertAfterAttempts = properties.alertAfterAttempts,
            lockKey = lockKey,
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
    ): EventPublisher = SnsEventPublisher(snsClient, aws.topicArn, settings.publishTimeout)

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
        properties: OutboxRelayProperties,
    ) = OutboxRelayScheduler(relay, settings.maxCycleDuration + settings.publishTimeout + SHUTDOWN_MARGIN, properties.pollInterval)

    private companion object {
        val SHUTDOWN_MARGIN: Duration = Duration.ofSeconds(1)
    }
}

/** `<prefix>.outbox.*`: relay tuning (see [OutboxRelaySettings]) and the poll interval read by [OutboxRelayScheduler]. */
data class OutboxRelayProperties(
    val batchSize: Int = 100,
    val alertAfterAttempts: Int = 10,
    val publishTimeout: Duration = Duration.ofSeconds(2),
    val maxCycleDuration: Duration = Duration.ofSeconds(10),
    val pollInterval: Duration = Duration.ofMillis(500),
)

/** `<prefix>.aws.*`: where the events go. A blank [endpoint] means the real AWS endpoint of [region]. */
data class AwsProperties(
    val endpoint: String? = null,
    val region: String,
    val topicArn: String,
)
