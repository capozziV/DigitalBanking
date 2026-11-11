package io.github.capozziv.digitalbanking.statement.application

import java.util.UUID

/** The statement knows neither the account (no `AccountOpened`) nor any entry of it. */
class AccountNotFoundException(
    val accountId: UUID,
) : RuntimeException("account $accountId not found")
