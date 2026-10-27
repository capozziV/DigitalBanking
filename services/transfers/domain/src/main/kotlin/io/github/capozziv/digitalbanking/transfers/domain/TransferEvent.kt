package io.github.capozziv.digitalbanking.transfers.domain

import java.util.UUID

sealed interface TransferEvent {
    val transferId: UUID

    data class TransferCompleted(
        override val transferId: UUID,
        val sourceAccountId: UUID,
        val destinationAccountId: UUID,
        val amount: Money,
        val description: String?,
    ) : TransferEvent

    data class TransferFailed(
        override val transferId: UUID,
        val sourceAccountId: UUID,
        val destinationAccountId: UUID,
        val amount: Money,
        val description: String?,
        val reason: String,
    ) : TransferEvent

    data class TransferCompensated(
        override val transferId: UUID,
        val sourceAccountId: UUID,
        val destinationAccountId: UUID,
        val amount: Money,
        val description: String?,
    ) : TransferEvent
}
