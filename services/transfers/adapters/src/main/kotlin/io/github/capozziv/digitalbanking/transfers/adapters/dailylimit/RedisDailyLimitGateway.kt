package io.github.capozziv.digitalbanking.transfers.adapters.dailylimit

import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitGateway
import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitUsage
import io.github.capozziv.digitalbanking.transfers.application.port.ReservationResult
import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.util.UUID

/**
 * Daily limit with the counter in Redis (Lua) and the source of truth in PostgreSQL ([JdbcDailyLimitStore]).
 *
 * Every reservation runs in its own transaction that starts with the lock of the account and day, so reservations of
 * the same account and day never overlap, whichever path they take:
 * - **Redis:** a counter seeded with another epoch than PostgreSQL's (or missing) is rebuilt from PostgreSQL, then
 *   `reserve.lua` decides; a reservation marks the transfer `limit_reserved` before the commit releases the lock.
 * - **PostgreSQL (any Redis failure):** sums the reservations, marks the transfer and bumps the epoch, which makes the
 *   next reservation through Redis rebuild the counter first.
 *
 * The Redis counter can only be above the real reservations (a release that failed, a reservation undone after the
 * Lua), never below; the TTL renewed by every reservation bounds how long.
 */
class RedisDailyLimitGateway(
    private val redis: StringRedisTemplate,
    private val scripts: DailyLimitScripts,
    private val store: JdbcDailyLimitStore,
    private val transactions: TransactionTemplate,
    private val properties: DailyLimitProperties,
    private val meterRegistry: MeterRegistry,
) : DailyLimitGateway {
    private val limit = Money(properties.amount)
    private val ttlSeconds = properties.counterTtl.seconds.toString()

    override fun reserve(
        transferId: UUID,
        accountId: UUID,
        day: LocalDate,
        amount: Money,
    ): ReservationResult {
        check(!TransactionSynchronizationManager.isActualTransactionActive()) {
            "the daily limit must be reserved outside a database transaction"
        }
        val outcome =
            try {
                checkNotNull(transactions.execute { reserveLocked(transferId, accountId, day, amount) })
            } catch (e: RuntimeException) {
                log.warn("daily limit unavailable for transfer {} (account {}, day {}): {}", transferId, accountId, day, e.message)
                countReservation(ERROR)
                return ReservationResult.UNAVAILABLE
            }
        countReservation(outcome.metric)
        return outcome.result
    }

    override fun release(
        transferId: UUID,
        accountId: UUID,
        day: LocalDate,
    ) {
        try {
            redisCall { redis.execute(scripts.release, listOf(key(accountId, day)), transferId.toString()) }
        } catch (e: RedisUnavailableException) {
            log.warn("daily limit release of transfer {} failed in redis (account {}, day {}): {}", transferId, accountId, day, e.message)
        }
    }

    override fun usage(
        accountId: UUID,
        day: LocalDate,
    ): DailyLimitUsage {
        val used =
            checkNotNull(
                transactions.execute {
                    store.lock(accountId, day)
                    val epoch = store.epoch(accountId, day)
                    try {
                        usedInRedis(accountId, day, epoch)
                    } catch (e: RedisUnavailableException) {
                        log.warn("daily limit usage of account {} on {} read from postgres: {}", accountId, day, e.message)
                        store.reservations(accountId, day).values.sum()
                    }
                },
            )
        return DailyLimitUsage(day, limit, used, (limit.cents - used).coerceAtLeast(0))
    }

    /** Runs under the lock of the account and day, inside the reservation's transaction. */
    private fun reserveLocked(
        transferId: UUID,
        accountId: UUID,
        day: LocalDate,
        amount: Money,
    ): Outcome {
        store.lock(accountId, day)
        val epoch = store.epoch(accountId, day)
        val reserved =
            try {
                reserveInRedis(transferId, accountId, day, amount, epoch)
            } catch (e: RedisUnavailableException) {
                log.warn("daily limit of transfer {} falls back to postgres (account {}, day {}): {}", transferId, accountId, day, e.message)
                return reserveInPostgres(transferId, accountId, day, amount)
            }
        if (!reserved) return Outcome.EXCEEDED
        store.markReserved(transferId)
        return Outcome.RESERVED
    }

    /** True when reserved (now or before). A missing key, even right after a rebuild, is rebuilt once more. */
    private fun reserveInRedis(
        transferId: UUID,
        accountId: UUID,
        day: LocalDate,
        amount: Money,
        epoch: Long,
    ): Boolean {
        val key = key(accountId, day)
        repeat(RESERVE_TRIES) {
            rebuildIfStale(key, accountId, day, epoch)
            val answer =
                redisCall {
                    redis.execute(scripts.reserve, listOf(key), transferId.toString(), amount.cents.toString(), limit.cents.toString(), ttlSeconds)
                }
            when (answer) {
                LUA_RESERVED -> return true
                LUA_EXCEEDED -> return false
            }
        }
        throw RedisUnavailableException("the counter $key went missing right after it was rebuilt")
    }

    private fun reserveInPostgres(
        transferId: UUID,
        accountId: UUID,
        day: LocalDate,
        amount: Money,
    ): Outcome {
        if (store.isReserved(transferId)) return Outcome.FALLBACK_RESERVED
        val used = store.reservations(accountId, day).values.sum()
        if (used + amount.cents > limit.cents) return Outcome.EXCEEDED
        store.markReserved(transferId)
        store.bumpEpoch(accountId, day)
        return Outcome.FALLBACK_RESERVED
    }

    private fun usedInRedis(
        accountId: UUID,
        day: LocalDate,
        epoch: Long,
    ): Long {
        val key = key(accountId, day)
        rebuildIfStale(key, accountId, day, epoch)
        return redisCall { redis.opsForHash<String, String>().get(key, TOTAL) }?.toLong()
            ?: throw RedisUnavailableException("the counter $key went missing right after it was rebuilt")
    }

    /** A counter missing, or seeded with another epoch than PostgreSQL's, is replaced by the reservations in PostgreSQL. */
    private fun rebuildIfStale(
        key: String,
        accountId: UUID,
        day: LocalDate,
        epoch: Long,
    ) {
        val stored = redisCall { redis.opsForHash<String, String>().get(key, EPOCH) }
        if (stored == epoch.toString()) return
        redisCall { redis.delete(key) }
        val pairs = store.reservations(accountId, day).flatMap { (id, cents) -> listOf(id.toString(), cents.toString()) }
        redisCall { redis.execute(scripts.seed, listOf(key), epoch.toString(), ttlSeconds, *pairs.toTypedArray()) }
        meterRegistry.counter(REBUILDS).increment()
    }

    /** Runs a Redis command, turning any failure into [RedisUnavailableException], so a PostgreSQL error never is one. */
    private fun <T> redisCall(command: () -> T): T =
        try {
            command()
        } catch (e: RuntimeException) {
            throw RedisUnavailableException(e.message ?: e.javaClass.simpleName)
        }

    private fun countReservation(result: String) = meterRegistry.counter(RESERVATIONS, RESULT, result).increment()

    private fun key(
        accountId: UUID,
        day: LocalDate,
    ) = "daily-limit:v1:$accountId:$day"

    private enum class Outcome(
        val result: ReservationResult,
        val metric: String,
    ) {
        RESERVED(ReservationResult.RESERVED, "reserved"),
        FALLBACK_RESERVED(ReservationResult.RESERVED, "fallback"),
        EXCEEDED(ReservationResult.EXCEEDED, "exceeded"),
    }

    private class RedisUnavailableException(
        message: String,
    ) : RuntimeException(message)

    private companion object {
        const val RESERVATIONS = "transfers.daily_limit.reservations"
        const val REBUILDS = "transfers.daily_limit.rebuilds"
        const val RESULT = "result"
        const val ERROR = "error"
        const val TOTAL = "total"
        const val EPOCH = "epoch"
        const val LUA_RESERVED = 1L
        const val LUA_EXCEEDED = 0L
        const val RESERVE_TRIES = 2
        val log = LoggerFactory.getLogger(RedisDailyLimitGateway::class.java)
    }
}
