package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.IdempotencyKeyReusedException
import io.github.capozziv.digitalbanking.transfers.application.port.AccountLookup
import io.github.capozziv.digitalbanking.transfers.application.port.AccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.port.CustomerGateway
import io.github.capozziv.digitalbanking.transfers.application.port.CustomerLookup
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

    /** The source account does not exist or is not the caller's: the same answer, so its existence does not leak. */
    data class SourceAccountNotFound(
        val accountId: UUID,
    ) : RequestTransferOutcome

    /** The destination account does not exist. */
    data class AccountNotFound(
        val accountId: UUID,
    ) : RequestTransferOutcome

    data object AccountsUnavailable : RequestTransferOutcome

    data object CustomerNotFound : RequestTransferOutcome

    data object CustomerBlocked : RequestTransferOutcome

    data object CustomersUnavailable : RequestTransferOutcome
}

/**
 * Accepts a transfer request: idempotent by key (same source, destination and amount; the description is not compared),
 * checks that the source account belongs to the caller (an admin may use any), that the destination exists and that
 * the customer that owns the source is active, then stores the transfer and runs the saga in the request.
 * The customer is asked every time (no cache): a blocked customer cannot transfer from the moment it is blocked.
 * No database transaction is open while `accounts` or `customer` is called.
 */
class RequestTransferUseCase(
    private val transfers: TransferRepository,
    private val accounts: AccountsGateway,
    private val customers: CustomerGateway,
    private val runSaga: RunTransferSagaUseCase,
    private val ids: IdGenerator,
    private val settings: SagaSettings,
    private val clock: Clock,
) {
    fun execute(command: RequestTransferCommand): RequestTransferOutcome {
        if (command.sourceAccountId == command.destinationAccountId) return RequestTransferOutcome.SameAccount
        transfers.findByIdempotencyKey(command.idempotencyKey)?.let { return repeat(it, command) }

        val sourceCustomerId =
            when (val source = accounts.lookupAccount(command.sourceAccountId)) {
                is AccountLookup.Found -> source.customerId
                AccountLookup.NotFound -> return RequestTransferOutcome.SourceAccountNotFound(command.sourceAccountId)
                AccountLookup.Unavailable -> return RequestTransferOutcome.AccountsUnavailable
            }
        if (!command.caller.mayAccess(sourceCustomerId)) return RequestTransferOutcome.SourceAccountNotFound(command.sourceAccountId)

        when (accounts.lookupAccount(command.destinationAccountId)) {
            is AccountLookup.Found -> Unit
            AccountLookup.NotFound -> return RequestTransferOutcome.AccountNotFound(command.destinationAccountId)
            AccountLookup.Unavailable -> return RequestTransferOutcome.AccountsUnavailable
        }

        when (customers.lookupCustomer(sourceCustomerId)) {
            CustomerLookup.ACTIVE -> Unit
            CustomerLookup.BLOCKED -> return RequestTransferOutcome.CustomerBlocked
            CustomerLookup.NOT_FOUND -> return RequestTransferOutcome.CustomerNotFound
            CustomerLookup.UNAVAILABLE -> return RequestTransferOutcome.CustomersUnavailable
        }

        val transfer =
            Transfer.request(
                id = ids.newId(),
                sourceAccountId = command.sourceAccountId,
                sourceCustomerId = sourceCustomerId,
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

    /**
     * Same key again. Someone else's transfer asked for with its own source is answered as a missing source (as a
     * new request would be); with another source, destination or amount the key is reused, whoever owns it.
     */
    private fun repeat(
        existing: Transfer,
        command: RequestTransferCommand,
    ): RequestTransferOutcome {
        if (!command.caller.mayAccess(existing.sourceCustomerId) && existing.sourceAccountId == command.sourceAccountId) {
            return RequestTransferOutcome.SourceAccountNotFound(command.sourceAccountId)
        }
        val same =
            existing.sourceAccountId == command.sourceAccountId &&
                existing.destinationAccountId == command.destinationAccountId &&
                existing.amount == command.amount
        if (!same) throw IdempotencyKeyReusedException(command.idempotencyKey)
        return RequestTransferOutcome.Existing(existing)
    }
}
