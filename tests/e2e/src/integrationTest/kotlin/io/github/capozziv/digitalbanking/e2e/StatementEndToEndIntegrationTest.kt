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
 * Stage 4 with `accounts`, `transfers` and `statement` running as real processes: the ledger entries of an account,
 * including the ones a transfer makes, reach the statement through the `accounts-events.fifo` topic and the
 * `statement-accounts.fifo` queue created by the LocalStack init script. Every wait is a condition polled with a time
 * limit, never a blind sleep.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatementEndToEndIntegrationTest {
    private val environment = EndToEndEnvironment()

    @RegisterExtension
    val processLogs = ProcessLogsOnFailureExtension { environment.processes() }

    private val accounts get() = environment.accountsApi
    private val transfers get() = environment.transfersApi
    private val statement get() = environment.statementApi

    @BeforeAll
    fun start() = environment.start()

    @AfterAll
    fun stop() = environment.close()

    @BeforeEach
    fun servicesRunning() {
        environment.proxy.releaseAll()
        environment.accounts.ensureRunning()
        environment.transfers.ensureRunning()
        environment.statement.ensureRunning()
    }

    @Test
    fun `the statements of both accounts show the credit, the debit and the transfer, newest first`() {
        val source = accounts.openAccount()
        val destination = accounts.openAccount()
        accounts.credit(source, 10_000)
        accounts.debit(source, 1_500)

        val id = transfers.requestTransfer(source, destination, 2_000).transferId()

        assertThat(transfers.awaitTerminal(id, SAGA_FINISH_TIMEOUT)).isEqualTo("COMPLETED")
        val sourceEntries = statement.awaitEntries(source, 3, STATEMENT_TIMEOUT)
        assertThat(sourceEntries.map { it.get("sequence").asLong() }).containsExactly(3L, 2L, 1L)
        assertThat(sourceEntries.map { it.get("kind").asString() }).containsExactly("DEBIT", "DEBIT", "CREDIT")
        assertThat(sourceEntries.map { it.get("amount").asLong() }).containsExactly(2_000L, 1_500L, 10_000L)
        assertThat(sourceEntries.map { it.get("balanceAfter").asLong() }).containsExactly(6_500L, 8_500L, 10_000L)
        val received = statement.awaitEntries(destination, 1, STATEMENT_TIMEOUT).single()
        assertThat(received.get("sequence").asLong()).isEqualTo(1L)
        assertThat(received.get("kind").asString()).isEqualTo("CREDIT")
        assertThat(received.get("amount").asLong()).isEqualTo(2_000L)
        assertThat(received.get("balanceAfter").asLong()).isEqualTo(2_000L)
        assertThat(accounts.balance(source)).isEqualTo(6_500L)
        assertThat(accounts.balance(destination)).isEqualTo(2_000L)
    }

    @Test
    fun `an account that was never opened has no statement`() {
        assertThat(statement.status(UUID.randomUUID())).isEqualTo(404)
    }

    private companion object {
        val SAGA_FINISH_TIMEOUT: Duration = Duration.ofSeconds(90)
        val STATEMENT_TIMEOUT: Duration = Duration.ofSeconds(30)
    }
}
