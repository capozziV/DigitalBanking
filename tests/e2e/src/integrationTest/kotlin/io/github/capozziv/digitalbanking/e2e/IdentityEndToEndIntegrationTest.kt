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
 * Stage 7 end to end, with the five services as real processes and the tokens of the `mock-oauth2-server`: a customer
 * registers, opens an account, gets a credit from an admin and transfers with their own token; another customer cannot
 * read that statement; an admin blocks the customer and the next transfer is refused by `transfers`, which asks
 * `customer` on every request. Every wait is a condition polled with a time limit, never a blind sleep.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdentityEndToEndIntegrationTest {
    private val environment = EndToEndEnvironment()

    @RegisterExtension
    val processLogs = ProcessLogsOnFailureExtension { environment.processes() }

    private val accounts get() = environment.accountsApi
    private val transfers get() = environment.transfersApi
    private val statement get() = environment.statementApi
    private val customers get() = environment.customerApi
    private val tokens get() = environment.tokens

    @BeforeAll
    fun start() = environment.start()

    @AfterAll
    fun stop() = environment.close()

    @BeforeEach
    fun servicesRunning() {
        environment.proxy.releaseAll()
        environment.processes().forEach { it.ensureRunning() }
    }

    @Test
    fun `a blocked customer cannot transfer and another customer cannot read the statement`() {
        val owner = UUID.randomUUID()
        val other = UUID.randomUUID()
        val source = accounts.openAccount(owner)
        val destination = accounts.openAccount(other)
        accounts.credit(source, 10_000)

        val first = transfers.requestTransfer(source, destination, 1_000, token = tokens.customer(owner))
        assertThat(first.status).describedAs(first.body).isEqualTo(201)
        assertThat(transfers.awaitTerminal(first.transferId(), SAGA_FINISH_TIMEOUT)).isEqualTo("COMPLETED")

        // The owner reads their statement; another customer gets 404 (never 403).
        statement.awaitEntries(source, 2, STATEMENT_TIMEOUT)
        assertThat(statement.status(source, tokens.customer(owner))).isEqualTo(200)
        assertThat(statement.status(source, tokens.customer(other))).isEqualTo(404)

        customers.changeStatus(owner, "BLOCKED")

        val refusedKey = UUID.randomUUID().toString()
        val refused =
            transfers.requestTransfer(source, destination, 1_000, idempotencyKey = refusedKey, token = tokens.customer(owner))
        assertThat(refused.status).describedAs(refused.body).isEqualTo(422)
        assertThat(refused.json().get("type").asString()).endsWith("/problems/customer-blocked")
        // Refused without creating the transfer: no id, no Location and no row for that idempotency key.
        assertThat(refused.json().has("id")).describedAs(refused.body).isFalse()
        assertThat(refused.json().has("transferId")).describedAs(refused.body).isFalse()
        assertThat(refused.headers.keys.map { it.lowercase() }).doesNotContain("location")
        assertThat(environment.transfersDatabase.transferCount(refusedKey)).isZero()
        // Nothing left the source: only the first transfer was debited.
        assertThat(accounts.balance(source)).isEqualTo(9_000L)
        assertThat(accounts.balance(destination)).isEqualTo(1_000L)
    }

    @Test
    fun `an admin blocks and unblocks an account, and closing one with balance is refused`() {
        val owner = UUID.randomUUID()
        val account = accounts.openAccount(owner)
        accounts.credit(account, 500)

        // The owner is a customer, not an admin: 403, and the account stays as it was.
        assertThat(accounts.changeStatusAsCustomer(account, owner, "BLOCKED").status).isEqualTo(403)

        val blocked = accounts.changeStatus(account, "BLOCKED")
        assertThat(blocked.status).describedAs(blocked.body).isEqualTo(200)
        assertThat(blocked.json().get("status").asString()).isEqualTo("BLOCKED")
        assertThat(accounts.changeStatus(account, "ACTIVE").status).isEqualTo(200)

        // Balance 500: refused before asking transfers.
        val notEmpty = accounts.close(account)
        assertThat(notEmpty.status).describedAs(notEmpty.body).isEqualTo(422)
        assertThat(notEmpty.json().get("type").asString()).endsWith("/problems/balance-not-zero")

        // Closing at zero balance asks the real transfers service (no pending transfers) and succeeds.
        val empty = accounts.openAccount()
        val closed = accounts.close(empty)
        assertThat(closed.status).describedAs(closed.body).isEqualTo(200)
        assertThat(closed.json().get("status").asString()).isEqualTo("CLOSED")
    }

    private companion object {
        val SAGA_FINISH_TIMEOUT: Duration = Duration.ofSeconds(90)
        val STATEMENT_TIMEOUT: Duration = Duration.ofSeconds(30)
    }
}
