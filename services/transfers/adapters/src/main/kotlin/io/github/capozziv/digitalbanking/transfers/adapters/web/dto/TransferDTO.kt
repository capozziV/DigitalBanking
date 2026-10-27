package io.github.capozziv.digitalbanking.transfers.adapters.web.dto

import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import io.github.capozziv.digitalbanking.transfers.domain.TransferStatus
import java.time.Instant
import java.util.UUID

/** Amount in cents. */
data class TransferDTO(
    val id: UUID,
    val sourceAccountId: UUID,
    val destinationAccountId: UUID,
    val amount: Long,
    val description: String?,
    val status: TransferStatus,
    val failureReason: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(transfer: Transfer) =
            TransferDTO(
                id = transfer.id,
                sourceAccountId = transfer.sourceAccountId,
                destinationAccountId = transfer.destinationAccountId,
                amount = transfer.amount.cents,
                description = transfer.description,
                status = transfer.status,
                failureReason = transfer.failureReason,
                createdAt = transfer.createdAt,
                updatedAt = transfer.updatedAt,
            )
    }
}
