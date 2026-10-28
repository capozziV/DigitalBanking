package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.domain.Money
import java.util.UUID

data class RequestTransferCommand(
    val sourceAccountId: UUID,
    val destinationAccountId: UUID,
    val amount: Money,
    val description: String?,
    val idempotencyKey: String,
    val correlationId: String?,
)
