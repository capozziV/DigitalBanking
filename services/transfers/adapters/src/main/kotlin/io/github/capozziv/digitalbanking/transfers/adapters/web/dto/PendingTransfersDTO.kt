package io.github.capozziv.digitalbanking.transfers.adapters.web.dto

/** How many PENDING transfers leave an account: the body accounts reads before closing it. */
data class PendingTransfersDTO(
    val count: Long,
)
