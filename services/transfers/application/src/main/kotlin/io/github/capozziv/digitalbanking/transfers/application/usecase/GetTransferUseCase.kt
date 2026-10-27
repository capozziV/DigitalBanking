package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import java.util.UUID

class GetTransferUseCase(
    private val transfers: TransferRepository,
) {
    fun execute(id: UUID): Transfer? = transfers.findById(id)
}
