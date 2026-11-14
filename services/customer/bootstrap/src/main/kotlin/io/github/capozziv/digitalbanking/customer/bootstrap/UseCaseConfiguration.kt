package io.github.capozziv.digitalbanking.customer.bootstrap

import io.github.capozziv.digitalbanking.customer.application.port.CustomerRepository
import io.github.capozziv.digitalbanking.customer.application.port.EventOutbox
import io.github.capozziv.digitalbanking.customer.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.customer.application.usecase.ChangeCustomerStatusUseCase
import io.github.capozziv.digitalbanking.customer.application.usecase.GetCustomerUseCase
import io.github.capozziv.digitalbanking.customer.application.usecase.GetOwnCustomerUseCase
import io.github.capozziv.digitalbanking.customer.application.usecase.RegisterCustomerUseCase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.Duration

/** Wires the use cases (plain Kotlin, no Spring) to the adapters. */
@Configuration(proxyBeanMethods = false)
class UseCaseConfiguration {
    /**
     * The single clock of the service, used by the use cases and by the outbox writer. It ticks in
     * microseconds, the precision of timestamptz, so an instant written and read back is unchanged.
     */
    @Bean
    fun clock(): Clock = Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000))

    @Bean
    fun registerCustomerUseCase(
        customers: CustomerRepository,
        outbox: EventOutbox,
        tx: TransactionRunner,
        clock: Clock,
    ) = RegisterCustomerUseCase(customers, outbox, tx, clock)

    @Bean
    fun getCustomerUseCase(
        customers: CustomerRepository,
        tx: TransactionRunner,
    ) = GetCustomerUseCase(customers, tx)

    @Bean
    fun getOwnCustomerUseCase(getCustomer: GetCustomerUseCase) = GetOwnCustomerUseCase(getCustomer)

    @Bean
    fun changeCustomerStatusUseCase(
        customers: CustomerRepository,
        outbox: EventOutbox,
        tx: TransactionRunner,
        clock: Clock,
    ) = ChangeCustomerStatusUseCase(customers, outbox, tx, clock)
}
