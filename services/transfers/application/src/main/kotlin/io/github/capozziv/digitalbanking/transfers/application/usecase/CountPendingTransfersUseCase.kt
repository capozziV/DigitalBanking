package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import java.util.UUID

/**
 * How many PENDING transfers leave an account. accounts asks before closing it: a transfer still running from the
 * account could debit it after the closing, or need a compensation credit that a closed account refuses.
 */
class CountPendingTransfersUseCase(
    private val transfers: TransferRepository,
) {
    fun execute(sourceAccountId: UUID): Long = transfers.countPendingFrom(sourceAccountId)
}
