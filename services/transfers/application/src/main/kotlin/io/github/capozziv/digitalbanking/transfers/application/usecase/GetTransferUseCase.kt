package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.security.Caller
import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import java.util.UUID

/** A transfer as seen by [Caller]: the customer that owns the source, or an admin. Anyone else gets null (404). */
class GetTransferUseCase(
    private val transfers: TransferRepository,
) {
    fun execute(
        id: UUID,
        caller: Caller,
    ): Transfer? = transfers.findById(id)?.takeIf { caller.mayAccess(it.sourceCustomerId) }
}
