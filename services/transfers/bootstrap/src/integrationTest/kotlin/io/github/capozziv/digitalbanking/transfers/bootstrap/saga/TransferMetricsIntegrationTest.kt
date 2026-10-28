package io.github.capozziv.digitalbanking.transfers.bootstrap.saga

import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import io.github.capozziv.digitalbanking.transfers.bootstrap.web.ApiClient
import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import io.github.capozziv.digitalbanking.transfers.domain.TransferStep
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * `transfers.pending` and `transfers.oldest.pending.age` show pending transfers on the actuator, so a compensation
 * refused in a loop (a bug) shows up as an old pending transfer. The scheduler is off here: the transfer stays pending.
 */
class TransferMetricsIntegrationTest : TransfersIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var repository: TransferRepository

    private val api by lazy { ApiClient(port) }

    @BeforeEach
    fun reset() {
        ownerJdbc.deleteTransfers()
    }

    @Test
    fun `with nothing pending the gauges are zero`() {
        repository.insert(transfer(Instant.now().minusSeconds(300), TransferStep.Completed))

        assertThat(metric("transfers.pending")).isZero()
        assertThat(metric("transfers.oldest.pending.age")).isZero()
    }

    @Test
    fun `a pending transfer created 90 s ago shows as one pending, 90 s old`() {
        repository.insert(transfer(Instant.now().minusSeconds(90), TransferStep.Compensate))
        repository.insert(transfer(Instant.now().minusSeconds(10), TransferStep.Credit))
        repository.insert(transfer(Instant.now().minusSeconds(600), TransferStep.Failed))

        assertThat(metric("transfers.pending")).isEqualTo(2.0)
        assertThat(metric("transfers.oldest.pending.age")).isCloseTo(90.0, within(5.0))
        assertThat(baseUnit("transfers.oldest.pending.age")).isEqualTo("seconds")
    }

    private fun metric(name: String): Double {
        val response = api.get("/actuator/metrics/$name")
        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        return response.body!!
            .get("measurements")
            .get(0)
            .get("value")
            .asDouble()
    }

    private fun baseUnit(name: String): String? =
        api
            .get("/actuator/metrics/$name")
            .body!!
            .get("baseUnit")
            ?.asString()

    private fun transfer(
        createdAt: Instant,
        step: TransferStep,
    ): Transfer {
        val at = createdAt.truncatedTo(ChronoUnit.MICROS)
        return Transfer(
            id = UUID.randomUUID(),
            sourceAccountId = UUID.randomUUID(),
            destinationAccountId = UUID.randomUUID(),
            amount = Money(100),
            description = null,
            idempotencyKey = "key-${UUID.randomUUID()}",
            step = step,
            failureReason = if (step == TransferStep.Failed) "x" else null,
            deadline = at.plus(Duration.ofMinutes(2)),
            attempts = 0,
            correlationId = null,
            createdAt = at,
            updatedAt = at,
        )
    }
}
