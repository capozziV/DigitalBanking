package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.IdempotencyKeyReusedException
import io.github.capozziv.digitalbanking.transfers.application.port.AccountLookup
import io.github.capozziv.digitalbanking.transfers.application.port.AccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.port.IdGenerator
import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import java.time.Clock
import java.util.UUID

sealed interface RequestTransferOutcome {
    data class Created(
        val transfer: Transfer,
    ) : RequestTransferOutcome

    data class Existing(
        val transfer: Transfer,
    ) : RequestTransferOutcome

    data object SameAccount : RequestTransferOutcome

    data class AccountNotFound(
        val accountId: UUID,
    ) : RequestTransferOutcome

    data object AccountsUnavailable : RequestTransferOutcome
}

/**
 * Accepts a transfer request: idempotent by key (same source, destination and amount; the description is not compared),
 * checks that both accounts exist, stores the transfer and runs the saga in the request.
 * No database transaction is open while `accounts` is called.
 */
class RequestTransferUseCase(
    private val transfers: TransferRepository,
    private val accounts: AccountsGateway,
    private val runSaga: RunTransferSagaUseCase,
    private val ids: IdGenerator,
    private val settings: SagaSettings,
    private val clock: Clock,
) {
    fun execute(command: RequestTransferCommand): RequestTransferOutcome {
        if (command.sourceAccountId == command.destinationAccountId) return RequestTransferOutcome.SameAccount
        transfers.findByIdempotencyKey(command.idempotencyKey)?.let { return repeat(it, command) }

        for (accountId in listOf(command.sourceAccountId, command.destinationAccountId)) {
            when (accounts.lookupAccount(accountId)) {
                AccountLookup.EXISTS -> Unit
                AccountLookup.NOT_FOUND -> return RequestTransferOutcome.AccountNotFound(accountId)
                AccountLookup.UNAVAILABLE -> return RequestTransferOutcome.AccountsUnavailable
            }
        }

        val transfer =
            Transfer.request(
                id = ids.newId(),
                sourceAccountId = command.sourceAccountId,
                destinationAccountId = command.destinationAccountId,
                amount = command.amount,
                description = command.description,
                idempotencyKey = command.idempotencyKey,
                correlationId = command.correlationId,
                now = clock.instant(),
                deadlineAfter = settings.deadline,
            )
        if (!transfers.insert(transfer)) {
            val winner = checkNotNull(transfers.findByIdempotencyKey(command.idempotencyKey)) { "idempotency key vanished after conflict" }
            return repeat(winner, command)
        }

        val afterSaga = runSaga.execute(transfer.id, "request-${ids.newId()}", settings.busyAttempts)
        // null: another run holds the lease; answer with what is persisted.
        val current = afterSaga ?: checkNotNull(transfers.findById(transfer.id)) { "transfer ${transfer.id} vanished" }
        return RequestTransferOutcome.Created(current)
    }

    private fun repeat(
        existing: Transfer,
        command: RequestTransferCommand,
    ): RequestTransferOutcome {
        val same =
            existing.sourceAccountId == command.sourceAccountId &&
                existing.destinationAccountId == command.destinationAccountId &&
                existing.amount == command.amount
        if (!same) throw IdempotencyKeyReusedException(command.idempotencyKey)
        return RequestTransferOutcome.Existing(existing)
    }
}
