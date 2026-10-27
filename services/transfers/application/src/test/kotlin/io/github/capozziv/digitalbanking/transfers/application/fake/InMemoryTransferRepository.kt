package io.github.capozziv.digitalbanking.transfers.application.fake

import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import java.time.Instant
import java.util.UUID

class InMemoryTransferRepository : TransferRepository {
    private data class Lease(
        val owner: String,
        val until: Instant,
    )

    private val transfers = LinkedHashMap<UUID, Transfer>()
    private val leases = HashMap<UUID, Lease>()
    private val nextAttempts = HashMap<UUID, Instant?>()
    private var savesUntilLeaseLoss: Int? = null

    var saveCount = 0
        private set

    /** Every `leaseUntil` passed to a successful [saveIfOwner], in order. */
    val renewedLeases = mutableListOf<Instant>()

    /** When true, [releaseLease] throws, leaving the lease in place as a crash would. */
    var failOnRelease = false

    fun nextAttemptAt(id: UUID): Instant? = nextAttempts[id]

    fun leaseOwner(id: UUID): String? = leases[id]?.owner

    /** Simulates another instance taking the lease over: the n-th call to [saveIfOwner] from now on returns false. */
    fun loseLeaseOnSave(n: Int) {
        savesUntilLeaseLoss = n
    }

    fun holdLease(
        id: UUID,
        owner: String,
        until: Instant,
    ) {
        leases[id] = Lease(owner, until)
    }

    override fun findById(id: UUID): Transfer? = transfers[id]

    override fun findByIdempotencyKey(key: String): Transfer? = transfers.values.firstOrNull { it.idempotencyKey == key }

    override fun insert(transfer: Transfer): Boolean {
        if (findByIdempotencyKey(transfer.idempotencyKey) != null) return false
        transfers[transfer.id] = transfer
        return true
    }

    override fun tryAcquireLease(
        id: UUID,
        owner: String,
        until: Instant,
        now: Instant,
    ): Boolean {
        val current = leases[id]
        if (current != null && current.owner != owner && current.until.isAfter(now)) return false
        leases[id] = Lease(owner, until)
        return true
    }

    override fun saveIfOwner(
        transfer: Transfer,
        owner: String,
        nextAttemptAt: Instant?,
        leaseUntil: Instant,
    ): Boolean {
        val remaining = savesUntilLeaseLoss
        if (remaining != null) {
            savesUntilLeaseLoss = remaining - 1
            if (remaining <= 1) {
                leases[transfer.id] = Lease("someone-else", Instant.MAX)
            }
        }
        if (leases[transfer.id]?.owner != owner) return false
        transfers[transfer.id] = transfer
        nextAttempts[transfer.id] = nextAttemptAt
        leases[transfer.id] = Lease(owner, leaseUntil)
        renewedLeases += leaseUntil
        saveCount++
        return true
    }

    override fun releaseLease(
        id: UUID,
        owner: String,
    ) {
        check(!failOnRelease) { "release failed" }
        if (leases[id]?.owner == owner) leases.remove(id)
    }

    /** Same semantics as the port: a transfer whose `nextAttemptAt` is null is never due. */
    override fun findDue(
        now: Instant,
        limit: Int,
    ): List<UUID> =
        transfers.values
            .filter { it.step.call(it) != null }
            .filter { leases[it.id]?.until?.isAfter(now) != true }
            .filter { nextAttempts[it.id]?.isAfter(now) == false }
            .take(limit)
            .map { it.id }
}
