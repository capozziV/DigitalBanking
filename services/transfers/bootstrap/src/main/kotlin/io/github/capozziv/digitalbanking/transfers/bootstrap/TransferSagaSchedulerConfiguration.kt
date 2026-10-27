package io.github.capozziv.digitalbanking.transfers.bootstrap

import io.github.capozziv.digitalbanking.transfers.application.usecase.ResumeDueTransfersUseCase
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Duration

/**
 * Runs [ResumeDueTransfersUseCase] every `transfers.saga.poll-interval`. Disabled with
 * `transfers.saga.scheduler-enabled=false` (the integration tests that do not exercise it).
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnBooleanProperty(name = ["transfers.saga.scheduler-enabled"], matchIfMissing = true)
class TransferSagaSchedulerConfiguration {
    /**
     * Waits on shutdown for one saga run at most: a run stops before its lease ends, and the rest of the batch is
     * resumed by another instance (or after restart) once the leases expire. Depends on `taskScheduler` (the bean
     * name of Spring Boot's scheduler) so that it is destroyed first: closing that scheduler interrupts the tick.
     */
    @Bean
    @DependsOn("taskScheduler")
    fun transferSagaScheduler(
        resumeDue: ResumeDueTransfersUseCase,
        properties: SagaProperties,
    ) = TransferSagaScheduler(resumeDue, properties.lease + SHUTDOWN_MARGIN)

    private companion object {
        val SHUTDOWN_MARGIN: Duration = Duration.ofSeconds(1)
    }
}
