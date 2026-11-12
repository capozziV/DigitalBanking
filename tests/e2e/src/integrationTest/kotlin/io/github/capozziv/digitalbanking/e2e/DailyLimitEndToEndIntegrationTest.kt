package io.github.capozziv.digitalbanking.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Duration
import java.util.UUID

/**
 * The daily limit (stage 6) with `accounts`, `transfers` and `notification` as real processes and the Redis of the
 * environment: the second of two transfers that together pass the limit is refused, the customer is told why, and
 * `GET /accounts/{id}/daily-limit` shows only the first.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DailyLimitEndToEndIntegrationTest {
    private val environment = EndToEndEnvironment()

    @RegisterExtension
    val processLogs = ProcessLogsOnFailureExtension { environment.processes() }

    private val accounts get() = environment.accountsApi
    private val transfers get() = environment.transfersApi
    private val notifications get() = environment.notificationApi

    @BeforeAll
    fun start() = environment.start()

    @AfterAll
    fun stop() = environment.close()

    @BeforeEach
    fun servicesRunning() {
        environment.proxy.releaseAll()
        environment.accounts.ensureRunning()
        environment.transfers.ensureRunning()
        environment.notification.ensureRunning()
    }

    @Test
    fun `two transfers above the daily limit - the second fails and the customer is notified`() {
        val sender = UUID.randomUUID()
        val source = accounts.openAccount(sender)
        notifications.awaitKinds(sender, listOf("WELCOME"), NOTIFICATION_TIMEOUT)
        val destination = accounts.openAccount()
        accounts.credit(source, 700_000)

        val first = transfers.requestTransfer(source, destination, 300_000)
        val second = transfers.requestTransfer(source, destination, 300_000)

        assertThat(first.status).describedAs(first.body).isEqualTo(201)
        assertThat(second.status).describedAs(second.body).isEqualTo(422)
        val secondId = second.transferId()
        assertThat(transfers.awaitTerminal(secondId, SAGA_FINISH_TIMEOUT)).isEqualTo("FAILED")
        assertThat(transfers.failureReason(secondId)).isEqualTo("daily-limit-exceeded")
        val failed = notifications.awaitKinds(sender, listOf("WELCOME", "TRANSFER_SENT", "TRANSFER_FAILED"), NOTIFICATION_TIMEOUT)
        assertThat(failed.single { it.get("kind").asString() == "TRANSFER_FAILED" }.get("body").asString())
            .contains("o limite diário de transferências da conta seria ultrapassado")
        val limit = transfers.dailyLimit(source)
        assertThat(limit.get("limitCents").asLong()).isEqualTo(500_000)
        assertThat(limit.get("usedCents").asLong()).isEqualTo(300_000)
        assertThat(limit.get("availableCents").asLong()).isEqualTo(200_000)
        // Nothing left the source for the refused transfer.
        assertThat(accounts.balance(source)).isEqualTo(400_000)
    }

    private companion object {
        val NOTIFICATION_TIMEOUT: Duration = Duration.ofSeconds(30)
        val SAGA_FINISH_TIMEOUT: Duration = Duration.ofSeconds(90)
    }
}
