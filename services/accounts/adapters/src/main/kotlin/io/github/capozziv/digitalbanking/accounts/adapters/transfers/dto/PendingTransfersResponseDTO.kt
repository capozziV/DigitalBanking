package io.github.capozziv.digitalbanking.accounts.adapters.transfers.dto

/** Body of `GET /internal/accounts/{id}/pending-transfers` of the `transfers` service. Nullable, so a missing field is detected. */
data class PendingTransfersResponseDTO(
    val count: Int?,
)
