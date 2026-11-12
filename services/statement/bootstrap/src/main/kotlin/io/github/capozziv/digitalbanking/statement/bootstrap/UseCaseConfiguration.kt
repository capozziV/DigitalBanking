package io.github.capozziv.digitalbanking.statement.bootstrap

import io.github.capozziv.digitalbanking.statement.application.port.StatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.application.port.StatementRepository
import io.github.capozziv.digitalbanking.statement.application.usecase.GetStatementUseCase
import io.github.capozziv.digitalbanking.statement.application.usecase.RecordStatementEntryUseCase
import io.github.capozziv.digitalbanking.statement.application.usecase.RegisterAccountUseCase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Wires the use cases (plain Kotlin, no Spring) to the adapters. */
@Configuration(proxyBeanMethods = false)
class UseCaseConfiguration {
    @Bean
    fun recordStatementEntryUseCase(
        repository: StatementRepository,
        cache: StatementFirstPageCache,
    ) = RecordStatementEntryUseCase(repository, cache)

    @Bean
    fun registerAccountUseCase(repository: StatementRepository) = RegisterAccountUseCase(repository)

    @Bean
    fun getStatementUseCase(
        repository: StatementRepository,
        cache: StatementFirstPageCache,
    ) = GetStatementUseCase(repository, cache)
}
