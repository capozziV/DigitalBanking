package io.github.capozziv.digitalbanking.transfers.application.fake

import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitGateway
import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitUsage
import io.github.capozziv.digitalbanking.transfers.application.port.ReservationResult
import io.github.capozziv.digitalbanking.transfers.domain.Money
import java.time.LocalDate
import java.util.UUID

/**
 * Daily limit kept in memory, per account and day. [reserve] is idempotent by transfer and answers `EXCEEDED` when the
 * reservations of the day would pass [limit]; [unavailable] makes every [reserve] answer `UNAVAILABLE`.
 */
class InMemoryDailyLimitGateway(
    private val limit: Money = Money(500_000),
    private val transactions: ImmediateTransactionRunner? = null,
) : DailyLimitGateway {
    private data class Reservation(
        val accountId: UUID,
        val day: LocalDate,
        val amount: Long,
    )

    private val reservations = LinkedHashMap<UUID, Reservation>()

    /** Every call to [reserve], as the transfer ID, in order. */
    val reserveCalls = mutableListOf<UUID>()

    /** Every call to [release], as the transfer ID, in order (also the ones that threw). */
    val releaseCalls = mutableListOf<UUID>()

    var unavailable = false

    /** When true, [release] throws after recording the call, as a broken Redis would. */
    var failOnRelease = false

    fun isReserved(transferId: UUID): Boolean = transferId in reservations

    override fun reserve(
        transferId: UUID,
        accountId: UUID,
        day: LocalDate,
        amount: Money,
    ): ReservationResult {
        check(transactions?.open != true) { "daily limit reserved inside a database transaction" }
        reserveCalls += transferId
        if (unavailable) return ReservationResult.UNAVAILABLE
        if (transferId in reservations) return ReservationResult.RESERVED
        if (used(accountId, day) + amount.cents > limit.cents) return ReservationResult.EXCEEDED
        reservations[transferId] = Reservation(accountId, day, amount.cents)
        return ReservationResult.RESERVED
    }

    override fun release(
        transferId: UUID,
        accountId: UUID,
        day: LocalDate,
    ) {
        check(transactions?.open != true) { "daily limit released inside a database transaction" }
        releaseCalls += transferId
        check(!failOnRelease) { "release failed" }
        reservations.remove(transferId)
    }

    override fun usage(
        accountId: UUID,
        day: LocalDate,
    ): DailyLimitUsage {
        val used = used(accountId, day)
        return DailyLimitUsage(day, limit, used, (limit.cents - used).coerceAtLeast(0))
    }

    private fun used(
        accountId: UUID,
        day: LocalDate,
    ): Long = reservations.values.filter { it.accountId == accountId && it.day == day }.sumOf { it.amount }
}
