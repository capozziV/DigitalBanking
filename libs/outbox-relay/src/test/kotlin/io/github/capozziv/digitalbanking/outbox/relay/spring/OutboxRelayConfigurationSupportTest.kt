package io.github.capozziv.digitalbanking.outbox.relay.spring

import io.github.capozziv.digitalbanking.outbox.relay.OutboxRelay
import io.github.capozziv.digitalbanking.outbox.relay.OutboxRelaySettings
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Duration

class OutboxRelayConfigurationSupportTest {
    private val runner =
        ApplicationContextRunner()
            .withUserConfiguration(InfrastructureConfiguration::class.java, DemoRelayConfiguration::class.java)

    private val withTopic =
        runner.withPropertyValues(
            "demo.aws.region=us-east-1",
            "demo.aws.topic-arn=arn:aws:sns:us-east-1:000000000000:demo.fifo",
        )

    @Test
    fun `wires the relay with the lock key of the service and the defaults when nothing is tuned`() {
        withTopic.run { context ->
            assertThat(context).hasNotFailed()
            val settings = context.getBean(OutboxRelaySettings::class.java)
            assertThat(settings.lockKey).isEqualTo(DEMO_LOCK_KEY)
            assertThat(settings.batchSize).isEqualTo(100)
            assertThat(settings.publishTimeout).isEqualTo(Duration.ofSeconds(2))
            assertThat(context).hasSingleBean(OutboxRelay::class.java)
            assertThat(context).hasSingleBean(OutboxRelayScheduler::class.java)
        }
    }

    @Test
    fun `reads the tuning from the property prefix of the service`() {
        withTopic
            .withPropertyValues("demo.outbox.batch-size=7", "demo.outbox.publish-timeout=3s", "other.outbox.batch-size=99")
            .run { context ->
                val settings = context.getBean(OutboxRelaySettings::class.java)
                assertThat(settings.batchSize).isEqualTo(7)
                assertThat(settings.publishTimeout).isEqualTo(Duration.ofSeconds(3))
            }
    }

    @Test
    fun `fails to start without the topic of the service`() {
        runner.run { context -> assertThat(context).hasFailed() }
    }

    @Configuration(proxyBeanMethods = false)
    class InfrastructureConfiguration {
        @Bean
        fun dataSource() = DriverManagerDataSource("jdbc:postgresql://localhost:1/none")

        @Bean
        fun jdbcClient(dataSource: DriverManagerDataSource) = JdbcClient.create(dataSource)

        @Bean
        fun transactionTemplate(dataSource: DriverManagerDataSource) = TransactionTemplate(DataSourceTransactionManager(dataSource))

        @Bean
        fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()

        @Bean
        fun clock(): Clock = Clock.systemUTC()

        @Bean
        fun taskScheduler() = ThreadPoolTaskScheduler()
    }

    @Configuration(proxyBeanMethods = false)
    class DemoRelayConfiguration : OutboxRelayConfigurationSupport(propertyPrefix = "demo", lockKey = DEMO_LOCK_KEY)

    private companion object {
        const val DEMO_LOCK_KEY = 0x64656D6FL
    }
}
