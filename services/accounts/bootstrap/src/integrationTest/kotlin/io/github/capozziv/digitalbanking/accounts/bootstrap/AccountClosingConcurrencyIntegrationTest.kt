package io.github.capozziv.digitalbanking.accounts.bootstrap

import io.github.capozziv.digitalbanking.accounts.bootstrap.web.ApiClient
import io.github.capozziv.digitalbanking.accounts.bootstrap.web.ApiResponse
import io.github.capozziv.digitalbanking.accounts.bootstrap.web.asCustomer
import io.github.capozziv.digitalbanking.accounts.bootstrap.web.assertProblem
import io.github.capozziv.digitalbanking.accounts.bootstrap.web.bearer
import io.github.capozziv.digitalbanking.accounts.bootstrap.web.retryWhileAccountBusy
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import java.sql.Connection
import java.sql.DriverManager
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Closing against debits and credits of the same account. The closing locks the account and re-reads status and
 * balance under the lock, so a debit after it is refused (422 account-closed) and a credit before it refuses it
 * (422 balance-not-zero). The deterministic cases hold the account lock in a transaction of the owner user, so the
 * requests queue on the row in a known order.
 */
class AccountClosingConcurrencyIntegrationTest : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }
    private val service = bearer(JwtTestSupport.serviceToken())

    @Test
    fun `a credit committed while the closing waits for the lock is seen, and the closing is 422 balance-not-zero`() {
        val (customerId, accountId) = openGoldAccount()

        val closing =
            holdingAccountLock(accountId) { connection ->
                val closing = async { close(accountId, customerId) }
                awaitLockWaiters(1)
                insertCredit(connection, accountId)
                closing
            }

        assertProblem(closing.get(30, TimeUnit.SECONDS), 422, "balance-not-zero")
        assertThat(statusOf(accountId)).isEqualTo("ACTIVE")
        assertThat(outboxCount(accountId, "AccountClosed")).isZero()
    }

    @Test
    fun `a debit queued behind the closing is 422 account-closed and records nothing`() {
        val (customerId, accountId) = openGoldAccount()

        val (closing, debit) =
            holdingAccountLock(accountId) {
                val closing = async { close(accountId, customerId) }
                awaitLockWaiters(1)
                val debit = async { debit(accountId, 100) }
                awaitLockWaiters(2)
                closing to debit
            }

        assertThat(closing.get(30, TimeUnit.SECONDS).status).isEqualTo(200)
        assertProblem(debit.get(30, TimeUnit.SECONDS), 422, "account-closed")
        assertThat(entryCount(accountId)).isZero()
        assertThat(statusOf(accountId)).isEqualTo("CLOSED")
    }

    @Test
    fun `20 parallel debits and a closing never leave a closed account with entries or a debit after the closing`() {
        val (customerId, accountId) = openGoldAccount()
        val start = CountDownLatch(1)

        val (closing, debits) =
            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                val debits = (1..20).map { executor.submit<ApiResponse> { start.await().let { debit(accountId, 1) } } }
                val closing = executor.submit<ApiResponse> { start.await().let { close(accountId, customerId) } }
                start.countDown()
                closing.get(60, TimeUnit.SECONDS) to debits.map { it.get(60, TimeUnit.SECONDS) }
            }

        val recorded = debits.count { it.status == 201 }
        debits.filter { it.status != 201 }.forEach { assertProblem(it, 422, "account-closed") }
        if (closing.status == 200) {
            assertThat(recorded).isZero()
            assertThat(statusOf(accountId)).isEqualTo("CLOSED")
        } else {
            assertProblem(closing, 422, "balance-not-zero")
            assertThat(recorded).isEqualTo(20)
            assertThat(statusOf(accountId)).isEqualTo("ACTIVE")
        }
        assertThat(entryCount(accountId)).isEqualTo(recorded.toLong())
    }

    // --- helpers ---

    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    private fun <T> async(block: () -> T): Future<T> = executor.submit<T> { block() }

    /** Runs [block] while a transaction of the owner user holds the lock of the account row, then commits it. */
    private fun <T> holdingAccountLock(
        accountId: String,
        block: (Connection) -> T,
    ): T =
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
            connection.autoCommit = false
            connection.prepareStatement("select id from account where id = ? for update").use {
                it.setObject(1, UUID.fromString(accountId))
                it.executeQuery().close()
            }
            val result = block(connection)
            connection.commit()
            result
        }

    /** Waits until [count] sessions of the application user wait for a row lock on the account table. */
    private fun awaitLockWaiters(count: Int) {
        await().atMost(Duration.ofSeconds(1)).pollInterval(Duration.ofMillis(10)).until {
            ownerJdbc
                .sql(
                    """
                    select count(*) from pg_stat_activity
                    where usename = :user and wait_event_type = 'Lock' and query ilike '%from account where id%for update%'
                    """.trimIndent(),
                ).param("user", APP_USER)
                .query(Long::class.java)
                .single() >= count
        }
    }

    private fun insertCredit(
        connection: Connection,
        accountId: String,
    ) {
        connection
            .prepareStatement(
                """
                insert into ledger_entry (id, account_id, sequence, kind, amount, balance_after, idempotency_key, created_at)
                values (?, ?, 1, 'CREDIT', 100, 100, ?, ?)
                """.trimIndent(),
            ).use {
                it.setObject(1, UUID.randomUUID())
                it.setObject(2, UUID.fromString(accountId))
                it.setString(3, UUID.randomUUID().toString())
                it.setObject(4, java.sql.Timestamp.from(Instant.now()))
                it.executeUpdate()
            }
    }

    /** A GOLD account (overdraft 2000.00) with zero balance: a debit would be accepted if the account were open. */
    private fun openGoldAccount(): Pair<UUID, String> {
        val customerId = UUID.randomUUID()
        val response = api.openAccount(customerId)
        assertThat(response.status).describedAs(response.rawBody).isEqualTo(201)
        val accountId = response.body!!.get("id").asString()
        assertThat(api.put("/accounts/$accountId/type", """{"type":"GOLD"}""").status).isEqualTo(200)
        return customerId to accountId
    }

    private fun close(
        accountId: String,
        customerId: UUID,
    ) = retryWhileAccountBusy { api.send(HttpMethod.POST, "/accounts/$accountId/close", headers = asCustomer(customerId)) }

    private fun debit(
        accountId: String,
        amount: Long,
    ): ApiResponse {
        val key = UUID.randomUUID().toString()
        return retryWhileAccountBusy {
            api.post("/accounts/$accountId/debits", """{"amount":$amount}""", mapOf("Idempotency-Key" to key) + service)
        }
    }

    private fun statusOf(accountId: String) =
        api
            .get("/accounts/$accountId")
            .body!!
            .get("status")
            .asString()

    private fun entryCount(accountId: String): Long =
        ownerJdbc
            .sql("select count(*) from ledger_entry where account_id = :id")
            .param("id", UUID.fromString(accountId))
            .query(Long::class.java)
            .single()

    private fun outboxCount(
        accountId: String,
        eventType: String,
    ): Long =
        ownerJdbc
            .sql("select count(*) from outbox where aggregate_id = :id and event_type = :type")
            .param("id", accountId)
            .param("type", eventType)
            .query(Long::class.java)
            .single()
}
