package io.github.capozziv.digitalbanking.transfers.application.fake

import io.github.capozziv.digitalbanking.transfers.application.port.StepRecord
import io.github.capozziv.digitalbanking.transfers.application.port.TransferStepLog

class InMemoryTransferStepLog : TransferStepLog {
    val records = mutableListOf<StepRecord>()

    override fun append(record: StepRecord) {
        records += record
    }
}
