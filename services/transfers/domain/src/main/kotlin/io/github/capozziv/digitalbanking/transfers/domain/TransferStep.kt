package io.github.capozziv.digitalbanking.transfers.domain

import java.time.Instant

/**
 * State pattern for the transfer saga: each step knows which call to make to `accounts`
 * and which step follows each outcome.
 *
 * | Step | Call | Success | Definitive refusal | Transient failure before deadline | Transient failure after deadline |
 * |---|---|---|---|---|---|
 * | `Debit` | debit on source, key `transfer-<id>-debit` | `Credit` | `Failed` (refusal reason) | `Debit` | `ResolveDebit` |
 * | `ResolveDebit` | find entry on source by the debit key | `Compensate` | `ENTRY_NOT_FOUND` -> `Failed` | `ResolveDebit` | `ResolveDebit` |
 * | `Credit` | credit on destination, key `transfer-<id>-credit` | `Completed` | `Compensate` | `Credit` | `ResolveCredit` |
 * | `ResolveCredit` | find entry on destination by the credit key | `Completed` | `ENTRY_NOT_FOUND` -> `Compensate` | `ResolveCredit` | `ResolveCredit` |
 * | `Compensate` | credit on source, key `transfer-<id>-compensation` | `Compensated` | `Compensate` (a refusal here is a bug) | `Compensate` | `Compensate` |
 * | `Completed`, `Failed`, `Compensated` | none (terminal) | - | - | - | - |
 *
 * The deadline only affects `Debit` and `Credit`; the resolve and compensate steps retry until they succeed.
 * A resolve step refused for any reason other than [StepOutcome.ENTRY_NOT_FOUND] stays in the same step.
 * Terminal steps reject every transition with [IllegalStateException].
 */
sealed interface TransferStep {
    val name: String
    val status: TransferStatus

    fun call(transfer: Transfer): AccountsCall?

    fun onSuccess(transfer: Transfer): TransferStep

    fun onRefused(
        transfer: Transfer,
        reason: String,
    ): TransferStep

    fun onTransientFailure(
        transfer: Transfer,
        now: Instant,
    ): TransferStep

    data object Debit : TransferStep {
        override val name = "DEBIT"
        override val status = TransferStatus.PENDING

        override fun call(transfer: Transfer) =
            AccountsCall.Debit(
                accountId = transfer.sourceAccountId,
                amount = transfer.amount,
                idempotencyKey = "transfer-${transfer.id}-debit",
                description = "Transfer to ${transfer.destinationAccountId}",
            )

        override fun onSuccess(transfer: Transfer): TransferStep = Credit

        override fun onRefused(
            transfer: Transfer,
            reason: String,
        ): TransferStep = Failed

        override fun onTransientFailure(
            transfer: Transfer,
            now: Instant,
        ): TransferStep = if (transfer.isPastDeadline(now)) ResolveDebit else Debit
    }

    data object ResolveDebit : TransferStep {
        override val name = "RESOLVE_DEBIT"
        override val status = TransferStatus.PENDING

        override fun call(transfer: Transfer) = AccountsCall.FindEntry(transfer.sourceAccountId, "transfer-${transfer.id}-debit")

        override fun onSuccess(transfer: Transfer): TransferStep = Compensate

        override fun onRefused(
            transfer: Transfer,
            reason: String,
        ): TransferStep = if (reason == StepOutcome.ENTRY_NOT_FOUND) Failed else ResolveDebit

        override fun onTransientFailure(
            transfer: Transfer,
            now: Instant,
        ): TransferStep = ResolveDebit
    }

    data object Credit : TransferStep {
        override val name = "CREDIT"
        override val status = TransferStatus.PENDING

        override fun call(transfer: Transfer) =
            AccountsCall.Credit(
                accountId = transfer.destinationAccountId,
                amount = transfer.amount,
                idempotencyKey = "transfer-${transfer.id}-credit",
                description = "Transfer from ${transfer.sourceAccountId}",
            )

        override fun onSuccess(transfer: Transfer): TransferStep = Completed

        override fun onRefused(
            transfer: Transfer,
            reason: String,
        ): TransferStep = Compensate

        override fun onTransientFailure(
            transfer: Transfer,
            now: Instant,
        ): TransferStep = if (transfer.isPastDeadline(now)) ResolveCredit else Credit
    }

    data object ResolveCredit : TransferStep {
        override val name = "RESOLVE_CREDIT"
        override val status = TransferStatus.PENDING

        override fun call(transfer: Transfer) = AccountsCall.FindEntry(transfer.destinationAccountId, "transfer-${transfer.id}-credit")

        override fun onSuccess(transfer: Transfer): TransferStep = Completed

        override fun onRefused(
            transfer: Transfer,
            reason: String,
        ): TransferStep = if (reason == StepOutcome.ENTRY_NOT_FOUND) Compensate else ResolveCredit

        override fun onTransientFailure(
            transfer: Transfer,
            now: Instant,
        ): TransferStep = ResolveCredit
    }

    data object Compensate : TransferStep {
        override val name = "COMPENSATE"
        override val status = TransferStatus.PENDING

        override fun call(transfer: Transfer) =
            AccountsCall.Credit(
                accountId = transfer.sourceAccountId,
                amount = transfer.amount,
                idempotencyKey = "transfer-${transfer.id}-compensation",
                description = "Transfer ${transfer.id} compensation",
            )

        override fun onSuccess(transfer: Transfer): TransferStep = Compensated

        override fun onRefused(
            transfer: Transfer,
            reason: String,
        ): TransferStep = Compensate

        override fun onTransientFailure(
            transfer: Transfer,
            now: Instant,
        ): TransferStep = Compensate
    }

    sealed interface Terminal : TransferStep {
        override fun call(transfer: Transfer): AccountsCall? = null

        override fun onSuccess(transfer: Transfer): TransferStep = error("$name is terminal")

        override fun onRefused(
            transfer: Transfer,
            reason: String,
        ): TransferStep = error("$name is terminal")

        override fun onTransientFailure(
            transfer: Transfer,
            now: Instant,
        ): TransferStep = error("$name is terminal")
    }

    data object Completed : Terminal {
        override val name = "COMPLETED"
        override val status = TransferStatus.COMPLETED
    }

    data object Failed : Terminal {
        override val name = "FAILED"
        override val status = TransferStatus.FAILED
    }

    data object Compensated : Terminal {
        override val name = "COMPENSATED"
        override val status = TransferStatus.COMPENSATED
    }

    companion object {
        private val all = listOf(Debit, ResolveDebit, Credit, ResolveCredit, Compensate, Completed, Failed, Compensated)

        fun fromName(name: String): TransferStep =
            all.firstOrNull { it.name == name } ?: throw IllegalArgumentException("unknown transfer step: $name")
    }
}
