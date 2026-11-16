package io.github.capozziv.digitalbanking.transfers.adapters.dailylimit

import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitGateway
import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitUsage
import io.github.capozziv.digitalbanking.transfers.application.port.ReservationResult
import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.util.Properties
import java.util.UUID

/**
 * Daily limit with the counter in Redis (Lua) and the source of truth in PostgreSQL ([JdbcDailyLimitStore]).
 *
 * Every reservation runs in its own transaction that starts with the lock of the account and day, so reservations of
 * the same account and day never overlap, whichever path they take:
 * - **Redis:** a counter seeded with another epoch than PostgreSQL's, by another Redis instance (`run_id`: a restart,
 *   a failover or an old snapshot may have lost writes) or missing is rebuilt from PostgreSQL, then `reserve.lua`
 *   decides; a reservation marks the transfer `limit_reserved` before the commit releases the lock.
 * - **PostgreSQL (any Redis failure, or the [circuit] open):** sums the reservations, marks the transfer and bumps the
 *   epoch, which makes the next reservation through Redis rebuild the counter first.
 *
 * The wait for the lock is bounded by [DailyLimitProperties.lockTimeout]; past it the reservation is unavailable and the
 * saga retries. When a reservation counted by `reserve.lua` is undone by PostgreSQL (rollback or failed commit), and
 * when a release fails in Redis, the epoch is bumped so the next reservation rebuilds the counter; the counter stays
 * above the real reservations only if that bump fails too, and then the TTL renewed by every reservation bounds how
 * long. It is never below them while the Redis instance keeps its writes.
 */
class RedisDailyLimitGateway(
    private val redis: StringRedisTemplate,
    private val scripts: DailyLimitScripts,
    private val store: JdbcDailyLimitStore,
    private val transactions: TransactionTemplate,
    private val properties: DailyLimitProperties,
    private val meterRegistry: MeterRegistry,
    private val circuit: RedisCircuitBreaker,
) : DailyLimitGateway {
    private val limit = Money(properties.amount)
    private val ttlSeconds = properties.counterTtl.seconds.toString()

    init {
        Gauge
            .builder(CIRCUIT_STATE, circuit) { it.state.code.toDouble() }
            .description("State of the circuit breaker of the daily limit in Redis: 0 closed, 1 open, 2 half-open")
            .register(meterRegistry)
    }

    override fun reserve(
        transferId: UUID,
        accountId: UUID,
        day: LocalDate,
        amount: Money,
    ): ReservationResult {
        check(!TransactionSynchronizationManager.isActualTransactionActive()) {
            "the daily limit must be reserved outside a database transaction"
        }
        var countedInRedis = false
        val outcome =
            try {
                checkNotNull(transactions.execute { reserveLocked(transferId, accountId, day, amount) { countedInRedis = true } })
            } catch (e: RuntimeException) {
                log.warn(
                    "daily limit unavailable for transfer {} (account {}, day {}): {}: {}",
                    transferId,
                    accountId,
                    day,
                    e.javaClass.simpleName,
                    e.message,
                )
                // The counter holds a reservation that PostgreSQL does not: make the next reservation rebuild it.
                if (countedInRedis) invalidateCounter(accountId, day)
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
            invalidateCounter(accountId, day)
        }
    }

    /**
     * The counter holds an amount PostgreSQL does not count, and the TTL renewed by every reservation could keep it for
     * the whole day. Bumping the epoch (no lock needed: the upsert is atomic) makes the next reservation rebuild it.
     */
    private fun invalidateCounter(
        accountId: UUID,
        day: LocalDate,
    ) {
        try {
            transactions.executeWithoutResult { store.bumpEpoch(accountId, day) }
        } catch (e: RuntimeException) {
            log.warn("daily limit counter of account {} on {} could not be invalidated: {}", accountId, day, e.message)
        }
    }

    override fun usage(
        accountId: UUID,
        day: LocalDate,
    ): DailyLimitUsage {
        // A value to show: read from the source of truth without the lock, so it never waits for nor delays reservations.
        val used = store.reservations(accountId, day).values.sum()
        return DailyLimitUsage(day, limit, used, (limit.cents - used).coerceAtLeast(0))
    }

    /** Runs under the lock of the account and day, inside the reservation's transaction. */
    private fun reserveLocked(
        transferId: UUID,
        accountId: UUID,
        day: LocalDate,
        amount: Money,
        onCountedInRedis: () -> Unit,
    ): Outcome {
        store.lock(accountId, day, properties.lockTimeout)
        val epoch = store.epoch(accountId, day)
        val reserved =
            try {
                reserveInRedis(transferId, accountId, day, amount, epoch, onCountedInRedis)
            } catch (e: RedisUnavailableException) {
                log.warn(
                    "daily limit of transfer {} falls back to postgres (account {}, day {}): {}",
                    transferId,
                    accountId,
                    day,
                    e.message,
                )
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
        onCountedInRedis: () -> Unit,
    ): Boolean {
        val key = key(accountId, day)
        repeat(RESERVE_TRIES) {
            rebuildIfStale(key, accountId, day, epoch)
            val answer =
                redisCall {
                    redis.execute(
                        scripts.reserve,
                        listOf(key),
                        transferId.toString(),
                        amount.cents.toString(),
                        limit.cents.toString(),
                        ttlSeconds,
                    )
                }
            when (answer) {
                LUA_RESERVED -> {
                    onCountedInRedis()
                    return true
                }

                LUA_EXCEEDED -> {
                    return false
                }
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

    /**
     * A counter missing, or seeded with another epoch than PostgreSQL's or by another Redis instance than the one that
     * answers now, is replaced by the reservations in PostgreSQL.
     */
    private fun rebuildIfStale(
        key: String,
        accountId: UUID,
        day: LocalDate,
        epoch: Long,
    ) {
        val instance = redisInstance()
        val stored = redisCall { redis.opsForHash<String, String>().multiGet(key, listOf(EPOCH, INSTANCE)) }
        if (stored == listOf(epoch.toString(), instance)) return
        redisCall { redis.delete(key) }
        val pairs = store.reservations(accountId, day).flatMap { (id, cents) -> listOf(id.toString(), cents.toString()) }
        redisCall { redis.execute(scripts.seed, listOf(key), epoch.toString(), instance, ttlSeconds, *pairs.toTypedArray()) }
        meterRegistry.counter(REBUILDS).increment()
    }

    /** The `run_id` of the Redis instance, new on every start and different on a replica promoted by a failover. */
    private fun redisInstance(): String =
        redisCall { redis.execute(RedisCallback<Properties> { it.serverCommands().info("server") }) }
            ?.getProperty(RUN_ID)
            ?: throw RedisUnavailableException("redis did not report its run_id")

    /**
     * Runs a Redis command through the [circuit], turning any failure into [RedisUnavailableException], so a
     * PostgreSQL error never is one. With the circuit open the command is not sent at all.
     */
    private fun <T> redisCall(command: () -> T): T {
        if (!circuit.tryAcquire()) throw RedisUnavailableException("the circuit breaker of redis is open")
        val result =
            try {
                command()
            } catch (e: RuntimeException) {
                circuit.onFailure()
                throw RedisUnavailableException(e.message ?: e.javaClass.simpleName)
            }
        circuit.onSuccess()
        return result
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
        const val CIRCUIT_STATE = "transfers.daily_limit.circuit.state"
        const val RESULT = "result"
        const val ERROR = "error"
        const val TOTAL = "total"
        const val EPOCH = "epoch"
        const val INSTANCE = "instance"
        const val RUN_ID = "run_id"
        const val LUA_RESERVED = 1L
        const val LUA_EXCEEDED = 0L
        const val RESERVE_TRIES = 2
        val log = LoggerFactory.getLogger(RedisDailyLimitGateway::class.java)
    }
}
