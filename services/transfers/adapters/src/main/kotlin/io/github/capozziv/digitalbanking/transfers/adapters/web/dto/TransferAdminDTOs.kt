package io.github.capozziv.digitalbanking.transfers.adapters.web.dto

import io.github.capozziv.digitalbanking.transfers.application.port.ManualResolution
import io.github.capozziv.digitalbanking.transfers.application.port.StuckTransfer
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

/** A transfer stuck in `COMPENSATE`: [refusal] is the reason `accounts` gave for the last compensation credit. */
data class StuckTransferDTO(
    val transfer: TransferDTO,
    val step: String,
    val attempts: Int,
    val refusal: String?,
    val refusedAt: Instant,
) {
    companion object {
        fun from(stuck: StuckTransfer) =
            StuckTransferDTO(
                transfer = TransferDTO.from(stuck.transfer),
                step = stuck.transfer.step.name,
                attempts = stuck.transfer.attempts,
                refusal = stuck.refusal,
                refusedAt = stuck.refusedAt,
            )
    }
}

/**
 * Why the admin compensates the transfer by hand, kept for audit, and the idempotency key of the credit the admin
 * recorded on the source account in `accounts`: it must be a credit of the transfer's amount there.
 */
data class CompensateManuallyDTO(
    // Nullable so that a missing value reaches bean validation and becomes a field error.
    @field:NotBlank @field:Size(max = MAX_REASON_LENGTH) val reason: String?,
    @field:NotBlank @field:Size(max = MAX_KEY_LENGTH) val creditIdempotencyKey: String?,
) {
    companion object {
        const val MAX_REASON_LENGTH = 500

        /** As `accounts` accepts an idempotency key. */
        const val MAX_KEY_LENGTH = 255
    }
}

/** The transfer compensated by hand, with who did it, when, why and with which credit. */
data class ManualCompensationDTO(
    val transfer: TransferDTO,
    val resolvedBy: UUID,
    val reason: String,
    val creditIdempotencyKey: String,
    val resolvedAt: Instant,
) {
    companion object {
        fun from(
            transfer: Transfer,
            resolution: ManualResolution,
        ) = ManualCompensationDTO(
            TransferDTO.from(transfer),
            resolution.resolvedBy,
            resolution.reason,
            resolution.creditIdempotencyKey,
            resolution.resolvedAt,
        )
    }
}
