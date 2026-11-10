package io.github.capozziv.digitalbanking.accounts.bootstrap

import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.AccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.port.EventOutbox
import io.github.capozziv.digitalbanking.accounts.application.port.IdGenerator
import io.github.capozziv.digitalbanking.accounts.application.port.LedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.accounts.application.usecase.ChangeAccountTypeUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.ChangeOverdraftLimitUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.FindEntryByIdempotencyKeyUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.GetAccountUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.ListAccountTypesUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.OpenAccountUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.RecordCreditUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.RecordDebitUseCase
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
    fun openAccountUseCase(
        accounts: AccountRepository,
        outbox: EventOutbox,
        tx: TransactionRunner,
        ids: IdGenerator,
    ) = OpenAccountUseCase(accounts, outbox, tx, ids)

    @Bean
    fun getAccountUseCase(
        accounts: AccountRepository,
        entries: LedgerEntryRepository,
        types: AccountTypeRepository,
    ) = GetAccountUseCase(accounts, entries, types)

    @Bean
    fun findEntryByIdempotencyKeyUseCase(
        accounts: AccountRepository,
        entries: LedgerEntryRepository,
    ) = FindEntryByIdempotencyKeyUseCase(accounts, entries)

    @Bean
    fun recordCreditUseCase(
        accounts: AccountRepository,
        entries: LedgerEntryRepository,
        outbox: EventOutbox,
        tx: TransactionRunner,
        ids: IdGenerator,
        clock: Clock,
    ) = RecordCreditUseCase(accounts, entries, outbox, tx, ids, clock)

    @Bean
    fun recordDebitUseCase(
        accounts: AccountRepository,
        entries: LedgerEntryRepository,
        types: AccountTypeRepository,
        outbox: EventOutbox,
        tx: TransactionRunner,
        ids: IdGenerator,
        clock: Clock,
    ) = RecordDebitUseCase(accounts, entries, types, outbox, tx, ids, clock)

    @Bean
    fun changeAccountTypeUseCase(
        accounts: AccountRepository,
        types: AccountTypeRepository,
        outbox: EventOutbox,
        tx: TransactionRunner,
    ) = ChangeAccountTypeUseCase(accounts, types, outbox, tx)

    @Bean
    fun listAccountTypesUseCase(types: AccountTypeRepository) = ListAccountTypesUseCase(types)

    @Bean
    fun changeOverdraftLimitUseCase(
        types: AccountTypeRepository,
        tx: TransactionRunner,
        clock: Clock,
    ) = ChangeOverdraftLimitUseCase(types, tx, clock)
}
