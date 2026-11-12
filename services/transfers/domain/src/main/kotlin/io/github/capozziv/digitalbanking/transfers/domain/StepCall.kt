package io.github.capozziv.digitalbanking.transfers.domain

import java.time.LocalDate
import java.util.UUID

/**
 * Call a saga step asks the application layer to make. The [Accounts] calls go to the `accounts` service;
 * [ReserveLimit] goes to the daily limit, inside `transfers`.
 */
sealed interface StepCall {
    /** A call answered by the `accounts` service. */
    sealed interface Accounts : StepCall

    data class Debit(
        val accountId: UUID,
        val amount: Money,
        val idempotencyKey: String,
        val description: String,
    ) : Accounts

    data class Credit(
        val accountId: UUID,
        val amount: Money,
        val idempotencyKey: String,
        val description: String,
    ) : Accounts

    data class FindEntry(
        val accountId: UUID,
        val idempotencyKey: String,
    ) : Accounts

    /** Reserves [amount] of the source account's daily limit on [day] for the transfer; idempotent by [transferId]. */
    data class ReserveLimit(
        val accountId: UUID,
        val day: LocalDate,
        val transferId: UUID,
        val amount: Money,
    ) : StepCall
}
