package io.github.capozziv.digitalbanking.accounts.bootstrap

import io.github.capozziv.digitalbanking.accounts.bootstrap.web.ApiClient
import io.github.capozziv.digitalbanking.accounts.bootstrap.web.ApiResponse
import io.github.capozziv.digitalbanking.accounts.bootstrap.web.assertProblem
import io.github.capozziv.digitalbanking.accounts.bootstrap.web.retryWhileAccountBusy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * End-to-end proof of the balance under concurrency and of idempotency (criteria 3 and 4 of the stage 1 spec):
 * parallel HTTP requests on one account, then the ledger and the outbox inspected as the owner user.
 */
class ConcurrencyIT : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }

    @Test
    fun `100 parallel debits of 100 on a FREE account credited with 3700 approve exactly 37 and refuse 63`() {
        val accountId = openAccount()
        assertThat(credit(accountId, 3_700, UUID.randomUUID().toString()).status).isEqualTo(201)

        val responses = inParallel(100) { debit(accountId, 100, UUID.randomUUID().toString()) }

        val statuses = responses.groupingBy { it.status }.eachCount()
        assertThat(statuses)
            .describedAs(responses.filter { it.status !in setOf(201, 422) }.map { it.rawBody }.toString())
            .containsExactlyInAnyOrderEntriesOf(mapOf(201 to 37, 422 to 63))
        responses.filter { it.status == 422 }.forEach { assertProblem(it, 422, "insufficient-available-balance") }
        assertThat(
            api
                .get("/accounts/$accountId")
                .body!!
                .get("balance")
                .asLong(),
        ).isZero()
        assertThat(sequences(accountId)).containsExactlyElementsOf((1L..38L).toList())
        assertThat(lastBalanceAfter(accountId)).isZero()
        assertThat(outboxCount(accountId, "LedgerEntryRecorded")).isEqualTo(entryCount(accountId)).isEqualTo(38)
    }

    @Test
    fun `20 parallel debits with the same key record a single entry, one 201 and nineteen 200 with the same body`() {
        val accountId = openAccount()
        assertThat(credit(accountId, 1_000, UUID.randomUUID().toString()).status).isEqualTo(201)
        val idempotencyKey = UUID.randomUUID().toString()

        val responses = inParallel(20) { debit(accountId, 100, idempotencyKey) }

        val statuses = responses.groupingBy { it.status }.eachCount()
        assertThat(statuses)
            .describedAs(responses.map { it.rawBody }.toString())
            .containsExactlyInAnyOrderEntriesOf(mapOf(201 to 1, 200 to 19))
        val created = responses.single { it.status == 201 }
        // The whole body, createdAt included: a retry returns the original entry, not a new one.
        responses.filter { it.status == 200 }.forEach { assertThat(it.rawBody).isEqualTo(created.rawBody) }
        assertThat(entryCount(accountId)).isEqualTo(2)
        assertThat(outboxCount(accountId, "LedgerEntryRecorded")).isEqualTo(2)
        assertThat(
            api
                .get("/accounts/$accountId")
                .body!!
                .get("balance")
                .asLong(),
        ).isEqualTo(900)
    }

    @Test
    fun `a debit refused for insufficient available balance can be retried with the same key after a credit and is recorded`() {
        val accountId = openAccount()
        val idempotencyKey = UUID.randomUUID().toString()

        val refused = debit(accountId, 500, idempotencyKey)
        assertProblem(refused, 422, "insufficient-available-balance")
        assertThat(credit(accountId, 500, UUID.randomUUID().toString()).status).isEqualTo(201)

        val retried = debit(accountId, 500, idempotencyKey)

        assertThat(retried.status).describedAs(retried.rawBody).isEqualTo(201)
        assertThat(retried.body!!.get("kind").asString()).isEqualTo("DEBIT")
        assertThat(retried.body.get("sequence").asLong()).isEqualTo(2)
        assertThat(retried.body.get("balanceAfter").asLong()).isZero()
        assertThat(entryCount(accountId)).isEqualTo(2)
        assertThat(outboxCount(accountId, "LedgerEntryRecorded")).isEqualTo(2)
    }

    private fun <T> inParallel(
        count: Int,
        request: () -> T,
    ): List<T> {
        val start = CountDownLatch(1)
        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val futures =
                (1..count).map {
                    executor.submit<T> {
                        start.await()
                        request()
                    }
                }
            start.countDown()
            return futures.map { it.get(60, TimeUnit.SECONDS) }
        }
    }

    private fun openAccount(): String {
        val response = api.post("/accounts", """{"customerId":"${UUID.randomUUID()}"}""", key(UUID.randomUUID().toString()))
        assertThat(response.status).isEqualTo(201)
        return response.body!!.get("id").asString()
    }

    private fun credit(
        accountId: String,
        amount: Long,
        idempotencyKey: String,
    ): ApiResponse = api.post("/accounts/$accountId/credits", """{"amount":$amount}""", key(idempotencyKey))

    /**
     * A debit as a well-behaved client sends it: a `503 account-busy` (lock timeout under contention) is
     * retried after `Retry-After` with the same Idempotency-Key, so the counts below do not depend on timing.
     */
    private fun debit(
        accountId: String,
        amount: Long,
        idempotencyKey: String,
    ): ApiResponse =
        retryWhileAccountBusy {
            api.post("/accounts/$accountId/debits", """{"amount":$amount}""", key(idempotencyKey))
        }

    private fun key(value: String) = mapOf("Idempotency-Key" to value)

    private fun sequences(accountId: String): List<Long> =
        ownerJdbc
            .sql("select sequence from ledger_entry where account_id = :id order by sequence")
            .param("id", UUID.fromString(accountId))
            .query(Long::class.java)
            .list()
            .map { checkNotNull(it) }

    private fun lastBalanceAfter(accountId: String): Long =
        ownerJdbc
            .sql("select balance_after from ledger_entry where account_id = :id order by sequence desc limit 1")
            .param("id", UUID.fromString(accountId))
            .query(Long::class.java)
            .single()

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
