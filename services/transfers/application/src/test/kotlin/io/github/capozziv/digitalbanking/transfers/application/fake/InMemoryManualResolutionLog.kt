package io.github.capozziv.digitalbanking.transfers.application.fake

import io.github.capozziv.digitalbanking.transfers.application.port.ManualResolution
import io.github.capozziv.digitalbanking.transfers.application.port.ManualResolutionLog
import java.util.UUID

/** One resolution per transfer, as the primary key of the table enforces. */
class InMemoryManualResolutionLog : ManualResolutionLog {
    val resolutions = LinkedHashMap<UUID, ManualResolution>()

    override fun append(resolution: ManualResolution) {
        check(resolution.transferId !in resolutions) { "transfer ${resolution.transferId} already resolved" }
        resolutions[resolution.transferId] = resolution
    }

    override fun find(transferId: UUID): ManualResolution? = resolutions[transferId]
}
