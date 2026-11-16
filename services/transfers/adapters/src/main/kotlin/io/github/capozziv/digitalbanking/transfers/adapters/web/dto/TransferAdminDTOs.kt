package io.github.capozziv.digitalbanking.transfers.adapters.web.dto

import io.github.capozziv.digitalbanking.transfers.application.port.ManualResolution
import io.github.capozziv.digitalbanking.transfers.application.usecase.StuckTransfer
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

/** Why the admin compensates the transfer by hand (what made the source account whole), kept for audit. */
data class CompensateManuallyDTO(
    // Nullable so that a missing value reaches bean validation and becomes a field error.
    @field:NotBlank @field:Size(max = MAX_REASON_LENGTH) val reason: String?,
) {
    companion object {
        const val MAX_REASON_LENGTH = 500
    }
}

/** The transfer compensated by hand, with who did it, when and why. */
data class ManualCompensationDTO(
    val transfer: TransferDTO,
    val resolvedBy: UUID,
    val reason: String,
    val resolvedAt: Instant,
) {
    companion object {
        fun from(
            transfer: Transfer,
            resolution: ManualResolution,
        ) = ManualCompensationDTO(TransferDTO.from(transfer), resolution.resolvedBy, resolution.reason, resolution.resolvedAt)
    }
}
