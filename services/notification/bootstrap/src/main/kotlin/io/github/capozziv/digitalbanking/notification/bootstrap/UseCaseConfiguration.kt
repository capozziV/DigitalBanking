package io.github.capozziv.digitalbanking.notification.bootstrap

import io.github.capozziv.digitalbanking.notification.application.port.IdGenerator
import io.github.capozziv.digitalbanking.notification.application.port.KnownAccountRepository
import io.github.capozziv.digitalbanking.notification.application.port.NotificationRepository
import io.github.capozziv.digitalbanking.notification.application.port.ProcessedEventRepository
import io.github.capozziv.digitalbanking.notification.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.notification.application.usecase.ConsumeEventUseCase
import io.github.capozziv.digitalbanking.notification.application.usecase.ListNotificationsUseCase
import io.github.capozziv.digitalbanking.notification.application.usecase.MarkNotificationsReadUseCase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.Duration

/** Wires the use cases (plain Kotlin, no Spring) to the adapters. */
@Configuration(proxyBeanMethods = false)
class UseCaseConfiguration {
    /** The single clock of the service; it ticks in microseconds, the precision of timestamptz. */
    @Bean
    fun clock(): Clock = Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000))

    @Bean
    fun consumeEventUseCase(
        processedEvents: ProcessedEventRepository,
        knownAccounts: KnownAccountRepository,
        notifications: NotificationRepository,
        transactions: TransactionRunner,
        ids: IdGenerator,
        clock: Clock,
    ) = ConsumeEventUseCase(processedEvents, knownAccounts, notifications, transactions, ids, clock)

    @Bean
    fun listNotificationsUseCase(notifications: NotificationRepository) = ListNotificationsUseCase(notifications)

    @Bean
    fun markNotificationsReadUseCase(
        notifications: NotificationRepository,
        transactions: TransactionRunner,
        clock: Clock,
    ) = MarkNotificationsReadUseCase(notifications, transactions, clock)
}
