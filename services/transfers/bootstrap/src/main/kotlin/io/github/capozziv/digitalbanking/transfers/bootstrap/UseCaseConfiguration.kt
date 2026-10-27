package io.github.capozziv.digitalbanking.transfers.bootstrap

import io.github.capozziv.digitalbanking.transfers.application.port.AccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.port.EventOutbox
import io.github.capozziv.digitalbanking.transfers.application.port.IdGenerator
import io.github.capozziv.digitalbanking.transfers.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.application.port.TransferStepLog
import io.github.capozziv.digitalbanking.transfers.application.usecase.GetTransferUseCase
import io.github.capozziv.digitalbanking.transfers.application.usecase.RequestTransferUseCase
import io.github.capozziv.digitalbanking.transfers.application.usecase.ResumeDueTransfersUseCase
import io.github.capozziv.digitalbanking.transfers.application.usecase.RetryBackoff
import io.github.capozziv.digitalbanking.transfers.application.usecase.RunTransferSagaUseCase
import io.github.capozziv.digitalbanking.transfers.application.usecase.SagaSettings
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.Duration
import java.util.UUID

/** Wires the use cases (plain Kotlin, no Spring) to the adapters. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SagaProperties::class)
class UseCaseConfiguration {
    /**
     * The single clock of the service, used by the use cases and by the outbox writer. It ticks in
     * microseconds, the precision of timestamptz, so an instant written and read back is unchanged.
     */
    @Bean
    fun clock(): Clock = Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000))

    @Bean
    fun sagaSettings(properties: SagaProperties): SagaSettings = properties.toSettings()

    @Bean
    fun retryBackoff(properties: SagaProperties): RetryBackoff = properties.toBackoff()

    @Bean
    fun runTransferSagaUseCase(
        transfers: TransferRepository,
        stepLog: TransferStepLog,
        accounts: AccountsGateway,
        outbox: EventOutbox,
        tx: TransactionRunner,
        backoff: RetryBackoff,
        settings: SagaSettings,
        clock: Clock,
    ) = RunTransferSagaUseCase(transfers, stepLog, accounts, outbox, tx, backoff, settings, clock)

    @Bean
    fun requestTransferUseCase(
        transfers: TransferRepository,
        accounts: AccountsGateway,
        runSaga: RunTransferSagaUseCase,
        ids: IdGenerator,
        settings: SagaSettings,
        clock: Clock,
    ) = RequestTransferUseCase(transfers, accounts, runSaga, ids, settings, clock)

    @Bean
    fun getTransferUseCase(transfers: TransferRepository) = GetTransferUseCase(transfers)

    /** The lease owner of the scheduler is unique per process; the scheduler itself is wired in a later task. */
    @Bean
    fun resumeDueTransfersUseCase(
        transfers: TransferRepository,
        runSaga: RunTransferSagaUseCase,
        settings: SagaSettings,
        clock: Clock,
    ) = ResumeDueTransfersUseCase(transfers, runSaga, settings, clock, "scheduler-${UUID.randomUUID()}")
}
