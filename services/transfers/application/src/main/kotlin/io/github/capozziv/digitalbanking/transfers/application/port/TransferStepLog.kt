package io.github.capozziv.digitalbanking.transfers.application.port

import java.time.Instant
import java.util.UUID

/** One row of the audit trail: every call the saga made to `accounts`, with its classified outcome. */
data class StepRecord(
    val transferId: UUID,
    val step: String,
    val outcome: String,
    val httpStatus: Int?,
    val detail: String?,
    val at: Instant,
)

interface TransferStepLog {
    fun append(record: StepRecord)
}
