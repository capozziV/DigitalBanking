package io.github.capozziv.digitalbanking.transfers.application.fake

import io.github.capozziv.digitalbanking.transfers.application.port.ManualResolution
import io.github.capozziv.digitalbanking.transfers.application.port.ManualResolutionLog
import java.util.UUID

/** One resolution per transfer and per credit key, as the primary key and the unique key of the table enforce. */
class InMemoryManualResolutionLog : ManualResolutionLog {
    val resolutions = LinkedHashMap<UUID, ManualResolution>()

    override fun append(resolution: ManualResolution): Boolean {
        check(resolution.transferId !in resolutions) { "transfer ${resolution.transferId} already resolved" }
        if (resolutions.values.any { it.creditIdempotencyKey == resolution.creditIdempotencyKey }) return false
        resolutions[resolution.transferId] = resolution
        return true
    }

    override fun find(transferId: UUID): ManualResolution? = resolutions[transferId]
}
