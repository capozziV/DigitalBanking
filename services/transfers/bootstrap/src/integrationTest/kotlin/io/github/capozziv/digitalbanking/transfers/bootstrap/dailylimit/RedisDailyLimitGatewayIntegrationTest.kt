package io.github.capozziv.digitalbanking.transfers.bootstrap.dailylimit

import io.github.capozziv.digitalbanking.testing.redis.RedisTestSupport
import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitGateway
import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitUsage
import io.github.capozziv.digitalbanking.transfers.application.port.ReservationResult
import io.github.capozziv.digitalbanking.transfers.application.port.ReservationResult.EXCEEDED
import io.github.capozziv.digitalbanking.transfers.application.port.ReservationResult.RESERVED
import io.github.capozziv.digitalbanking.transfers.application.port.ReservationResult.UNAVAILABLE
import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import io.github.capozziv.digitalbanking.transfers.bootstrap.persistence.aTransfer
import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.TransferStep
import io.micrometer.core.instrument.MeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import java.sql.DriverManager
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * The daily limit against a real Redis and PostgreSQL, with the default limit of 500000 cents. Every reservation is
 * for a transfer that exists in the `transfer` table, since a reservation also marks it `limit_reserved`.
 */
class RedisDailyLimitGatewayIntegrationTest : TransfersIntegrationTest() {
    @Autowired
    private lateinit var gateway: DailyLimitGateway

    @Autowired
    private lateinit var transfers: TransferRepository

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    private val account = UUID.randomUUID()
    private val day = LocalDate.parse("2026-11-12")

    @Test
    fun `reserves while under the limit and refuses above it`() {
        val first = transferOf(300_000)
        val second = transferOf(300_000)
        val third = transferOf(200_000)
        val reserved = counter("reserved")
        val exceeded = counter("exceeded")

        assertThat(reserve(first, 300_000)).isEqualTo(RESERVED)
        assertThat(reserve(second, 300_000)).isEqualTo(EXCEEDED)
        assertThat(reserve(third, 200_000)).isEqualTo(RESERVED)

        assertThat(limitReserved(first)).isTrue()
        assertThat(limitReserved(second)).isFalse()
        assertThat(limitReserved(third)).isTrue()
        assertThat(counter("reserved")).isEqualTo(reserved + 2)
        assertThat(counter("exceeded")).isEqualTo(exceeded + 1)
    }

    @Test
    fun `the same transfer reserved twice counts once`() {
        val first = transferOf(300_000)
        val second = transferOf(200_000)

        assertThat(reserve(first, 300_000)).isEqualTo(RESERVED)
        assertThat(reserve(first, 300_000)).isEqualTo(RESERVED)
        assertThat(reserve(second, 200_000)).isEqualTo(RESERVED)

        assertThat(gateway.usage(account, day).used).isEqualTo(500_000)
    }

    @Test
    fun `concurrent reservations never exceed the limit`() {
        // Several rounds, each on a new account whose counter is not in Redis yet, so every round races the rebuild too.
        repeat(ROUNDS) {
            val accountId = UUID.randomUUID()
            val ids = List(20) { transferOf(50_000, accountId) }

            val results = concurrently(ids.map { id -> { reserve(id, 50_000, accountId) } })

            assertThat(results.count { it == RESERVED }).isEqualTo(10)
            assertThat(results.count { it == EXCEEDED }).isEqualTo(10)
            assertThat(reservedSumInPostgres(accountId)).isEqualTo(500_000)
        }
    }

    @Test
    fun `a reservation waits for the lock of its account and day, and only for that one`() {
        val waiting = transferOf(1_000)
        val otherAccount = UUID.randomUUID()
        val elsewhere = transferOf(1_000, otherAccount)
        val pool = Executors.newSingleThreadExecutor()
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
            connection.autoCommit = false
            connection
                .prepareStatement("select pg_advisory_xact_lock(hashtextextended(? || ':' || ?, 0))")
                .use { lock ->
                    lock.setString(1, account.toString())
                    lock.setString(2, day.toString())
                    lock.executeQuery().close()
                }
            try {
                val blocked = pool.submit<ReservationResult> { reserve(waiting, 1_000) }

                assertThat(reserve(elsewhere, 1_000, otherAccount)).isEqualTo(RESERVED)
                assertThatThrownBy { blocked.get(500, TimeUnit.MILLISECONDS) }.isInstanceOf(TimeoutException::class.java)
                connection.commit()
                assertThat(blocked.get(10, TimeUnit.SECONDS)).isEqualTo(RESERVED)
            } finally {
                connection.rollback()
                pool.shutdownNow()
            }
        }
    }

    @Test
    fun `the lock is taken before the epoch is read, so a reservation that waited sees the fallback's epoch`() {
        reserve(transferOf(50_000), 50_000)
        val behindFallback = transferOf(400_000)
        val waiting = transferOf(200_000)
        val pool = Executors.newSingleThreadExecutor()
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
            connection.autoCommit = false
            connection
                .prepareStatement("select pg_advisory_xact_lock(hashtextextended(? || ':' || ?, 0))")
                .use { lock ->
                    lock.setString(1, account.toString())
                    lock.setString(2, day.toString())
                    lock.executeQuery().close()
                }
            try {
                val blocked = pool.submit<ReservationResult> { reserve(waiting, 200_000) }
                awaitBlockedOnAdvisoryLock()

                // What the fallback does while it holds the lock: mark the transfer and bump the epoch.
                connection.prepareStatement("update transfer set limit_reserved = true where id = ?").use {
                    it.setObject(1, behindFallback)
                    it.executeUpdate()
                }
                connection
                    .prepareStatement("insert into daily_limit_epoch (account_id, limit_day, epoch) values (?, ?, 1)")
                    .use {
                        it.setObject(1, account)
                        it.setObject(2, day)
                        it.executeUpdate()
                    }
                connection.commit()

                assertThat(blocked.get(10, TimeUnit.SECONDS)).isEqualTo(EXCEEDED)
                assertThat(redisTemplate.opsForHash<String, String>().get(key(), "total")).isEqualTo("450000")
            } finally {
                connection.rollback()
                pool.shutdownNow()
            }
        }
    }

    @Test
    fun `an idempotent reservation renews the ttl of the counter`() {
        val transfer = transferOf(1_000)
        reserve(transfer, 1_000)
        redisTemplate.expire(key(), java.time.Duration.ofSeconds(5))

        assertThat(reserve(transfer, 1_000)).isEqualTo(RESERVED)

        assertThat(redisTemplate.getExpire(key(), TimeUnit.SECONDS)).isBetween(590L, 600L)
    }

    @Test
    fun `release returns the amount and is idempotent`() {
        val first = transferOf(300_000)
        val second = transferOf(200_000)
        val third = transferOf(300_000)
        val fourth = transferOf(300_000)
        reserve(first, 300_000)
        reserve(second, 200_000)
        // The saga releases after saving the terminal state: the source of truth stops counting the transfer first.
        fail(first)

        gateway.release(first, account, day)
        gateway.release(first, account, day)

        assertThat(reserve(third, 300_000)).isEqualTo(RESERVED)
        assertThat(reserve(fourth, 300_000)).isEqualTo(EXCEEDED)
        assertThat(gateway.usage(account, day).used).isEqualTo(500_000)
    }

    @Test
    fun `a missing key is rebuilt from postgres`() {
        val first = transferOf(400_000)
        val second = transferOf(200_000)
        reserve(first, 400_000)
        redisTemplate.delete(key())
        val rebuilds = rebuilds()

        assertThat(reserve(second, 200_000)).isEqualTo(EXCEEDED)

        assertThat(rebuilds()).isEqualTo(rebuilds + 1)
        assertThat(redisTemplate.opsForHash<String, String>().get(key(), "total")).isEqualTo("400000")
        assertThat(redisTemplate.opsForHash<String, String>().get(key(), first.toString())).isEqualTo("400000")
    }

    @Test
    fun `the redis key expires after the counter ttl`() {
        reserve(transferOf(1_000), 1_000)

        assertThat(redisTemplate.getExpire(key(), TimeUnit.SECONDS)).isBetween(590L, 600L)
    }

    @Test
    fun `with redis paused, reservations fall back to postgres and bump the epoch`() {
        val warm = transferOf(100_000)
        val second = transferOf(300_000)
        val third = transferOf(200_000)
        reserve(warm, 100_000)
        val fallback = counter("fallback")
        val exceeded = counter("exceeded")

        val results =
            whilePaused {
                listOf(reserve(second, 300_000), reserve(third, 200_000))
            }

        assertThat(results).containsExactly(RESERVED, EXCEEDED)
        assertThat(limitReserved(second)).isTrue()
        assertThat(limitReserved(third)).isFalse()
        assertThat(epochInPostgres()).isEqualTo(1)
        assertThat(counter("fallback")).isEqualTo(fallback + 1)
        assertThat(counter("exceeded")).isEqualTo(exceeded + 1)
    }

    @Test
    fun `a transfer already reserved is reserved again by the fallback without counting twice`() {
        val first = transferOf(300_000)
        reserve(first, 300_000)

        val again = whilePaused { reserve(first, 300_000) }

        assertThat(again).isEqualTo(RESERVED)
        assertThat(reservedSumInPostgres()).isEqualTo(300_000)
        assertThat(epochInPostgres()).isZero()
    }

    @Test
    fun `mixed paths never exceed the limit`() {
        // Four batches of six reservations of 50000; the second and the fourth run with Redis paused.
        val results = mutableListOf<ReservationResult>()
        repeat(4) { batch ->
            val ids = List(6) { transferOf(50_000) }
            val tasks = ids.map { id -> { reserve(id, 50_000) } }
            results += if (batch % 2 == 1) whilePaused { concurrently(tasks) } else concurrently(tasks)
        }

        assertThat(results.count { it == RESERVED }).isEqualTo(10)
        assertThat(reservedSumInPostgres()).isEqualTo(500_000)
    }

    @Test
    fun `after reservations through postgres, the redis counter is rebuilt before deciding`() {
        // The counter exists in Redis before the outage, so only the epoch tells it is stale.
        reserve(transferOf(50_000), 50_000)
        val throughPostgres = transferOf(400_000)
        val afterwards = transferOf(200_000)
        assertThat(whilePaused { reserve(throughPostgres, 400_000) }).isEqualTo(RESERVED)
        val rebuilds = rebuilds()

        assertThat(reserve(afterwards, 200_000)).isEqualTo(EXCEEDED)

        assertThat(rebuilds()).isEqualTo(rebuilds + 1)
        assertThat(redisTemplate.opsForHash<String, String>().get(key(), "epoch")).isEqualTo("1")
        assertThat(redisTemplate.opsForHash<String, String>().get(key(), "total")).isEqualTo("450000")
    }

    @Test
    fun `usage reports limit, used and available`() {
        reserve(transferOf(120_000), 120_000)
        val expected = DailyLimitUsage(day, Money(500_000), used = 120_000, available = 380_000)

        assertThat(gateway.usage(account, day)).isEqualTo(expected)
        assertThat(whilePaused { gateway.usage(account, day) }).isEqualTo(expected)
        assertThat(gateway.usage(UUID.randomUUID(), day))
            .isEqualTo(DailyLimitUsage(day, Money(500_000), used = 0, available = 500_000))
    }

    @Test
    fun `a reservation for an unknown transfer is unavailable and counted as an error`() {
        val errors = counter("error")

        assertThat(reserve(UUID.randomUUID(), 1_000)).isEqualTo(UNAVAILABLE)

        assertThat(counter("error")).isEqualTo(errors + 1)
    }

    private fun reserve(
        transferId: UUID,
        amount: Long,
        accountId: UUID = account,
    ): ReservationResult = gateway.reserve(transferId, accountId, day, Money(amount))

    /** Inserts a pending transfer of [amount] from [accountId] on [day], at the step that reserves the limit. */
    private fun transferOf(
        amount: Long,
        accountId: UUID = account,
    ): UUID {
        val transfer =
            aTransfer(step = TransferStep.ReserveLimit, limitDay = day)
                .copy(sourceAccountId = accountId, amount = Money(amount))
        transfers.insert(transfer)
        return transfer.id
    }

    /** Waits until a backend is blocked on an advisory lock, i.e. the reservation reached the lock. */
    private fun awaitBlockedOnAdvisoryLock() {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            val waiting =
                ownerJdbc
                    .sql("select count(*) from pg_stat_activity where wait_event_type = 'Lock' and wait_event = 'advisory'")
                    .query(Long::class.java)
                    .single()
            if (waiting > 0) return
            Thread.sleep(20)
        }
        error("no reservation got blocked on the advisory lock")
    }

    private fun fail(transferId: UUID) {
        ownerJdbc
            .sql("update transfer set status = 'FAILED', step = 'FAILED' where id = :id")
            .param("id", transferId)
            .update()
    }

    private fun limitReserved(transferId: UUID): Boolean =
        ownerJdbc
            .sql("select limit_reserved from transfer where id = :id")
            .param("id", transferId)
            .query(Boolean::class.java)
            .single()

    private fun reservedSumInPostgres(accountId: UUID = account): Long =
        ownerJdbc
            .sql(
                "select coalesce(sum(amount), 0) from transfer " +
                    "where source_account_id = :account and limit_day = :day and limit_reserved",
            ).param("account", accountId)
            .param("day", day)
            .query(Long::class.java)
            .single()

    private fun epochInPostgres(): Long =
        ownerJdbc
            .sql("select coalesce(max(epoch), 0) from daily_limit_epoch where account_id = :account and limit_day = :day")
            .param("account", account)
            .param("day", day)
            .query(Long::class.java)
            .single()

    private fun key() = "daily-limit:v1:$account:$day"

    private fun counter(result: String): Double =
        meterRegistry
            .find("transfers.daily_limit.reservations")
            .tag("result", result)
            .counter()
            ?.count() ?: 0.0

    private fun rebuilds(): Double = meterRegistry.find("transfers.daily_limit.rebuilds").counter()?.count() ?: 0.0

    private fun <T> whilePaused(block: () -> T): T {
        RedisTestSupport.pause(redis)
        try {
            return block()
        } finally {
            RedisTestSupport.unpause(redis)
        }
    }

    /** Runs every task on its own thread, all released at once, and returns the results in the tasks' order. */
    private fun <T> concurrently(tasks: List<() -> T>): List<T> {
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(tasks.size)
        try {
            val futures = tasks.map { task -> pool.submit<T> { start.await().let { task() } } }
            start.countDown()
            return futures.map { it.get(60, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
        }
    }

    private companion object {
        const val ROUNDS = 5
    }
}
