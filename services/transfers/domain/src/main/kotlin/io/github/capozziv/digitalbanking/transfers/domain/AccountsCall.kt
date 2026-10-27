package io.github.capozziv.digitalbanking.transfers.domain

import java.util.UUID

/** Call a saga step asks the application layer to make to `accounts`. */
sealed interface AccountsCall {
    data class Debit(
        val accountId: UUID,
        val amount: Money,
        val idempotencyKey: String,
        val description: String,
    ) : AccountsCall

    data class Credit(
        val accountId: UUID,
        val amount: Money,
        val idempotencyKey: String,
        val description: String,
    ) : AccountsCall

    data class FindEntry(
        val accountId: UUID,
        val idempotencyKey: String,
    ) : AccountsCall
}
