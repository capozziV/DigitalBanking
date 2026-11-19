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
    private val environment = EndToEndEnvironment(welcomeBonusCents = WELCOME_BONUS)

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
    fun `the statements of both accounts show the welcome bonus, the credit, the debit and the transfer, newest first`() {
        val source = accounts.openAccount()
        val destination = accounts.openAccount()
        accounts.credit(source, 10_000)
        accounts.debit(source, 1_500)

        val id = transfers.requestTransfer(source, destination, 2_000).transferId()

        assertThat(transfers.awaitTerminal(id, SAGA_FINISH_TIMEOUT)).isEqualTo("COMPLETED")
        val sourceEntries = statement.awaitEntries(source, 4, STATEMENT_TIMEOUT)
        assertThat(sourceEntries.map { it.get("sequence").asLong() }).containsExactly(4L, 3L, 2L, 1L)
        assertThat(sourceEntries.map { it.get("kind").asString() }).containsExactly("DEBIT", "DEBIT", "CREDIT", "CREDIT")
        assertThat(sourceEntries.map { it.get("amount").asLong() }).containsExactly(2_000L, 1_500L, 10_000L, WELCOME_BONUS)
        assertThat(sourceEntries.map { it.get("balanceAfter").asLong() }).containsExactly(16_500L, 18_500L, 20_000L, WELCOME_BONUS)
        assertThat(sourceEntries[0].get("description").asString()).isEqualTo("Transfer to $destination")
        val received = statement.awaitEntries(destination, 2, STATEMENT_TIMEOUT).first()
        assertThat(received.get("sequence").asLong()).isEqualTo(2L)
        assertThat(received.get("kind").asString()).isEqualTo("CREDIT")
        assertThat(received.get("description").asString()).isEqualTo("Transfer from $source")
        assertThat(received.get("amount").asLong()).isEqualTo(2_000L)
        assertThat(received.get("balanceAfter").asLong()).isEqualTo(WELCOME_BONUS + 2_000L)
        assertThat(accounts.balance(source)).isEqualTo(16_500L)
        assertThat(accounts.balance(destination)).isEqualTo(WELCOME_BONUS + 2_000L)
    }

    @Test
    fun `an account that was never opened has no statement`() {
        assertThat(statement.status(UUID.randomUUID())).isEqualTo(404)
    }

    @Test
    fun `a read before a transfer does not hide the new debit`() {
        val source = accounts.openAccount()
        val destination = accounts.openAccount()
        accounts.credit(source, 10_000)
        // Wait for the credit with a read that skips the cache (limit=100). Polling the cached first page with a long
        // TTL could pin an empty page: a read may see the table before the consume and cache it after the evict (the
        // accepted race of the spec, bounded by the TTL).
        statement.awaitEntries(source, 2, STATEMENT_TIMEOUT)
        // One single cached read warms the first-page cache of the source with the bonus and the credit only.
        assertThat(statement.firstPage(source)).hasSize(2)

        val id = transfers.requestTransfer(source, destination, 2_000).transferId()

        assertThat(transfers.awaitTerminal(id, SAGA_FINISH_TIMEOUT)).isEqualTo("COMPLETED")
        // Below the 50 s minimum cache TTL: only the eviction on consume can make the debit show in time.
        val entries = statement.awaitFirstPage(source, 3, CACHE_EVICTION_TIMEOUT)
        assertThat(entries.map { it.get("kind").asString() }).containsExactly("DEBIT", "CREDIT", "CREDIT")
    }

    private companion object {
        const val WELCOME_BONUS = 10_000L
        val SAGA_FINISH_TIMEOUT: Duration = Duration.ofSeconds(90)
        val STATEMENT_TIMEOUT: Duration = Duration.ofSeconds(30)
        val CACHE_EVICTION_TIMEOUT: Duration = Duration.ofSeconds(30)
    }
}
