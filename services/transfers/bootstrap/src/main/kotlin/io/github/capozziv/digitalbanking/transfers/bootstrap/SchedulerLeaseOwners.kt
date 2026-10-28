package io.github.capozziv.digitalbanking.transfers.bootstrap

import java.net.InetAddress
import java.util.UUID

/**
 * Lease owners of the saga scheduler, `scheduler-<hostname>-<pid>-<uuid>`: one per saga run, never per process,
 * because taking the lease is reentrant for the same owner. Host and pid only make the owner readable in the table.
 */
class SchedulerLeaseOwners(
    private val hostname: String = localHostname(),
    private val pid: Long = ProcessHandle.current().pid(),
) {
    fun next(): String = "scheduler-$hostname-$pid-${UUID.randomUUID()}"

    private companion object {
        fun localHostname(): String = runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("unknown-host")
    }
}
