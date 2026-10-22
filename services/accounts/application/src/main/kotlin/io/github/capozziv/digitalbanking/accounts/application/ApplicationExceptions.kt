package io.github.capozziv.digitalbanking.accounts.application

import java.util.UUID

class AccountNotFoundException(
    val accountId: UUID,
) : RuntimeException("account $accountId not found")

class AccountBusyException(
    val accountId: UUID,
) : RuntimeException("account $accountId is busy")

class IdempotencyKeyReusedException(
    val key: String,
) : RuntimeException("idempotency key $key was already used for another operation")

class CustomerAlreadyHasAccountException(
    val customerId: UUID,
) : RuntimeException("customer $customerId already has an account")
