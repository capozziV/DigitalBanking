package io.github.capozziv.digitalbanking.statement.application.usecase

import io.github.capozziv.digitalbanking.statement.application.port.StatementRepository
import java.util.UUID

class RegisterAccountUseCase(
    private val repository: StatementRepository,
) {
    /** Idempotent: registering a known account again does nothing. */
    fun execute(
        accountId: UUID,
        customerId: UUID,
    ) {
        repository.insertAccount(accountId, customerId)
    }
}
