package io.github.capozziv.digitalbanking.transfers.bootstrap.dailylimit

import io.github.capozziv.digitalbanking.testing.redis.RedisTestSupport
import io.github.capozziv.digitalbanking.transfers.adapters.dailylimit.DailyLimitProperties
import io.github.capozziv.digitalbanking.transfers.adapters.dailylimit.DailyLimitScripts
import io.github.capozziv.digitalbanking.transfers.adapters.dailylimit.JdbcDailyLimitStore
import io.github.capozziv.digitalbanking.transfers.adapters.dailylimit.RedisCircuitBreaker
import io.github.capozziv.digitalbanking.transfers.adapters.dailylimit.RedisDailyLimitGateway
import io.github.capozziv.digitalbanking.transfers.application.port.ReservationResult
import io.github.capozziv.digitalbanking.transfers.application.port.ReservationResult.RESERVED
import io.github.capozziv.digitalbanking.transfers.application.port.ReservationResult.UNAVAILABLE
import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import io.github.capozziv.digitalbanking.transfers.bootstrap.persistence.aTransfer
import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.TransferStep
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.transaction.support.TransactionTemplate
import java.sql.DriverManager
import java.time.Duration
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * The circuit breaker and the lock timeout of the daily limit, on a gateway of its own (circuit with a threshold of 2,
 * a manual clock and a lock timeout of 200 ms), so the circuit state never leaks into the gateway of the context.
 */
class RedisDailyLimitGatewayResilienceIntegrationTest : TransfersIntegrationTest() {
    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @Autowired
    private lateinit var jdbc: JdbcClient

    @Autowired
    private lateinit var transactions: TransactionTemplate

    @Autowired
    private lateinit var transfers: TransferRepository

    private var now = 0L
    private val registry = SimpleMeterRegistry()
    private val circuit = RedisCircuitBreaker(failureThreshold = 2, openDuration = OPEN_DURATION) { now }
    private val account = UUID.randomUUID()
    private val day = LocalDate.parse("2026-11-12")

    private val gateway by lazy {
        RedisDailyLimitGateway(
            redisTemplate,
            DailyLimitScripts(),
            JdbcDailyLimitStore(jdbc),
            transactions,
            DailyLimitProperties(lockTimeout = Duration.ofMillis(200)),
            registry,
            circuit,
        )
    }

    @Test
    fun `after the threshold of redis failures the circuit opens and reservations skip redis`() {
        assertThat(reserve(transferOf(1_000))).isEqualTo(RESERVED)
        assertThat(circuitState()).isEqualTo(0.0)

        whilePaused {
            assertThat(reserve(transferOf(1_000))).isEqualTo(RESERVED)
            assertThat(reserve(transferOf(1_000))).isEqualTo(RESERVED)
        }
        assertThat(circuitState()).isEqualTo(1.0)

        // Redis is back, but the circuit is still open: the reservation is decided in PostgreSQL.
        assertThat(reserve(transferOf(1_000))).isEqualTo(RESERVED)
        assertThat(counter("reserved")).isEqualTo(1.0)
        assertThat(counter("fallback")).isEqualTo(3.0)
    }

    @Test
    fun `after the open duration a probe through redis closes the circuit`() {
        reserve(transferOf(1_000))
        whilePaused { repeat(2) { reserve(transferOf(1_000)) } }
        assertThat(circuitState()).isEqualTo(1.0)

        now += OPEN_DURATION.toNanos()

        assertThat(reserve(transferOf(1_000))).isEqualTo(RESERVED)
        assertThat(circuitState()).isEqualTo(0.0)
        assertThat(counter("reserved")).isEqualTo(2.0)
        assertThat(redisTemplate.opsForHash<String, String>().get(key(), "total")).isEqualTo("4000")
    }

    @Test
    fun `a probe that fails while redis is still down opens the circuit again and the reservation falls back to postgres`() {
        reserve(transferOf(1_000))
        whilePaused { repeat(2) { reserve(transferOf(1_000)) } }
        assertThat(circuitState()).isEqualTo(1.0)
        now += OPEN_DURATION.toNanos()

        whilePaused { assertThat(reserve(transferOf(1_000))).isEqualTo(RESERVED) }

        assertThat(circuitState()).isEqualTo(1.0)
        // Redis is back, but the failed probe opened the circuit for another open duration.
        assertThat(reserve(transferOf(1_000))).isEqualTo(RESERVED)
        assertThat(counter("reserved")).isEqualTo(1.0)
        assertThat(counter("fallback")).isEqualTo(4.0)
        now += OPEN_DURATION.toNanos()
        assertThat(reserve(transferOf(1_000))).isEqualTo(RESERVED)
        assertThat(circuitState()).isEqualTo(0.0)
        assertThat(counter("reserved")).isEqualTo(2.0)
    }

    @Test
    fun `a reservation that waits longer than the lock timeout is unavailable`() {
        val waiting = transferOf(1_000)
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
                val started = System.nanoTime()
                val result = pool.submit<ReservationResult> { reserve(waiting) }

                assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo(UNAVAILABLE)
                // The 200 ms of the properties, not the 2 s of the default: inside the saga's call budget.
                assertThat(Duration.ofNanos(System.nanoTime() - started)).isBetween(Duration.ofMillis(190), Duration.ofMillis(1_500))
                assertThat(counter("error")).isEqualTo(1.0)
            } finally {
                connection.rollback()
                pool.shutdownNow()
            }
        }
    }

    private fun reserve(transferId: UUID): ReservationResult = gateway.reserve(transferId, account, day, Money(1_000))

    private fun transferOf(amount: Long): UUID {
        val transfer =
            aTransfer(step = TransferStep.ReserveLimit, limitDay = day)
                .copy(sourceAccountId = account, amount = Money(amount))
        transfers.insert(transfer)
        return transfer.id
    }

    private fun key() = "daily-limit:v1:$account:$day"

    private fun circuitState(): Double? = registry.find("transfers.daily_limit.circuit.state").gauge()?.value()

    private fun counter(result: String): Double =
        registry
            .find("transfers.daily_limit.reservations")
            .tag("result", result)
            .counter()
            ?.count() ?: 0.0

    private fun <T> whilePaused(block: () -> T): T {
        RedisTestSupport.pause(redis)
        try {
            return block()
        } finally {
            RedisTestSupport.unpause(redis)
        }
    }

    private companion object {
        val OPEN_DURATION: Duration = Duration.ofMinutes(1)
    }
}
