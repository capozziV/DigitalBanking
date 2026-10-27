package io.github.capozziv.digitalbanking.transfers.adapters.web

import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import java.util.UUID

/** A field that failed validation, as listed in `errors` of the validation-error problem. */
data class InvalidField(
    val field: String,
    val message: String,
)

class InvalidRequestException(
    val errors: List<InvalidField>,
) : RuntimeException("invalid request: ${errors.joinToString { it.field }}")

class TransferNotFoundException(
    val id: UUID,
) : RuntimeException("transfer $id not found")

class SameAccountTransferException : RuntimeException("source and destination accounts must differ")

class TransferAccountNotFoundException(
    val accountId: UUID,
) : RuntimeException("account $accountId not found")

class AccountsUnavailableException : RuntimeException("accounts is unavailable")

/** The transfer ended in `FAILED`: the request is answered with 422, on the first call and on every repetition. */
class TransferFailedException(
    val transfer: Transfer,
) : RuntimeException("transfer ${transfer.id} failed: ${transfer.failureReason}")
