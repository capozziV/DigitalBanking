package io.github.capozziv.digitalbanking.accounts.adapters.web

import io.github.capozziv.digitalbanking.accounts.domain.AvailableBalance
import java.util.UUID

/** A field that failed validation, as listed in `errors` of the validation-error problem. */
data class InvalidField(
    val field: String,
    val message: String,
)

class InvalidRequestException(
    val errors: List<InvalidField>,
) : RuntimeException("invalid request: ${errors.joinToString { it.field }}")

class MissingIdempotencyKeyException : RuntimeException("Idempotency-Key header is required")

class AccountTypeNotFoundException(
    val type: String,
) : RuntimeException("account type $type not found")

class InsufficientAvailableBalanceException(
    val accountId: UUID,
    val availableBalance: AvailableBalance,
) : RuntimeException("available balance of account $accountId does not cover the debit")
