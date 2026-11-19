package io.github.capozziv.digitalbanking.accounts.bootstrap.web

import io.github.capozziv.digitalbanking.accounts.bootstrap.AccountsIntegrationTest
import io.github.capozziv.digitalbanking.accounts.bootstrap.TransfersServiceStub.Behavior
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.util.UUID

/** `POST /accounts/{id}/close`: who may close, what blocks the closing, and what a closed account still does. */
class AccountClosingApiIntegrationTest : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }
    private val service = bearer(JwtTestSupport.serviceToken())
    private val mapper = JsonMapper.builder().build()

    @Test
    fun `the owner closes its account with zero balance, asking transfers with the service token and the correlation id`() {
        val (customerId, accountId) = openAccount()

        val response = close(accountId, asCustomer(customerId) + mapOf("X-Correlation-Id" to "close-test-1"))

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("status").asString()).isEqualTo("CLOSED")
        assertThat(response.body.get("balance").asLong()).isZero()
        assertThat(
            api
                .get("/accounts/$accountId", asCustomer(customerId))
                .body!!
                .get("status")
                .asString(),
        ).isEqualTo("CLOSED")
        val events = outboxRows(accountId, "AccountClosed")
        assertThat(events).hasSize(1)
        val (payload, correlationId) = events.single()
        assertThat(payload.get("customerId").asString()).isEqualTo(customerId.toString())
        assertThat(payload.get("occurredAt").asString()).isNotBlank()
        assertThat(correlationId).isEqualTo("close-test-1")
        val lookup = transfersService.lookupsOf(UUID.fromString(accountId)).single()
        assertThat(lookup.getHeader("X-Correlation-Id")).isEqualTo("close-test-1")
        val scope =
            JwtTestSupport
                .jwtDecoder()
                .decode(lookup.getHeader("Authorization").removePrefix("Bearer "))
                .getClaimAsString("scope")!!
        assertThat(scope.split(" ")).contains(JwtTestSupport.SERVICE_SCOPE)
    }

    @Test
    fun `an admin closes the account of any customer`() {
        val (_, accountId) = openAccount()

        assertThat(close(accountId, bearer(JwtTestSupport.adminToken(UUID.randomUUID()))).status).isEqualTo(200)
    }

    @Test
    fun `another customer gets 404 account-not-found, a service 403, and the account stays active`() {
        val (_, accountId) = openAccount()

        assertProblem(close(accountId, asCustomer(UUID.randomUUID())), 404, "account-not-found")
        assertProblem(close(accountId, service), 403, "forbidden")
        assertThat(statusOf(accountId)).isEqualTo("ACTIVE")
        assertThat(transfersService.lookupsOf(UUID.fromString(accountId))).isEmpty()
    }

    @Test
    fun `a balance other than zero is 422 balance-not-zero`() {
        val (customerId, accountId) = openAccount()
        api.post("/accounts/$accountId/credits", """{"amount":100}""", key() + service)

        assertProblem(close(accountId, asCustomer(customerId)), 422, "balance-not-zero")
        assertThat(statusOf(accountId)).isEqualTo("ACTIVE")
    }

    @Test
    fun `a pending outgoing transfer is 422 pending-transfers`() {
        val (customerId, accountId) = openAccount()
        transfersService.answer(UUID.fromString(accountId), Behavior.Pending(3))

        val response = close(accountId, asCustomer(customerId))

        assertProblem(response, 422, "pending-transfers")
        assertThat(response.body!!.get("count").asInt()).isEqualTo(3)
        assertThat(statusOf(accountId)).isEqualTo("ACTIVE")
        assertThat(outboxRows(accountId, "AccountClosed")).isEmpty()
    }

    @Test
    fun `transfers answering an error is 503 transfers-unavailable and the account stays active`() {
        val (customerId, accountId) = openAccount()
        transfersService.answer(UUID.fromString(accountId), Behavior.ServerError)

        assertProblem(close(accountId, asCustomer(customerId)), 503, "transfers-unavailable")
        assertThat(statusOf(accountId)).isEqualTo("ACTIVE")
    }

    @Test
    fun `transfers slower than the 500 ms timeout is 503 transfers-unavailable, and the account stays active`() {
        val (customerId, accountId) = openAccount()
        transfersService.answer(UUID.fromString(accountId), Behavior.Slow(Duration.ofSeconds(3)))

        val started = System.nanoTime()
        val response = close(accountId, asCustomer(customerId))
        val elapsed = Duration.ofNanos(System.nanoTime() - started)

        assertProblem(response, 503, "transfers-unavailable")
        // The read timeout (500 ms by default) cuts the 3 s answer; the margin is for the request itself.
        assertThat(elapsed).isBetween(Duration.ofMillis(500), Duration.ofMillis(850))
        assertThat(statusOf(accountId)).isEqualTo("ACTIVE")
        assertThat(outboxRows(accountId, "AccountClosed")).isEmpty()
    }

    @Test
    fun `closing twice is 422 account-closed without a second event`() {
        val (customerId, accountId) = openAccount()
        assertThat(close(accountId, asCustomer(customerId)).status).isEqualTo(200)

        assertProblem(close(accountId, asCustomer(customerId)), 422, "account-closed")
        assertThat(outboxRows(accountId, "AccountClosed")).hasSize(1)
    }

    @Test
    fun `a blocked account with zero balance can be closed`() {
        val (customerId, accountId) = openAccount()
        api.put("/admin/accounts/$accountId/status", """{"status":"BLOCKED"}""")

        assertThat(close(accountId, asCustomer(customerId)).status).isEqualTo(200)
    }

    @Test
    fun `a closed account refuses debits and credits with 422 account-closed, and its status cannot change`() {
        val (customerId, accountId) = openAccount("GOLD")
        close(accountId, asCustomer(customerId))

        assertProblem(api.post("/accounts/$accountId/debits", """{"amount":100}""", key() + service), 422, "account-closed")
        assertProblem(api.post("/accounts/$accountId/credits", """{"amount":100}""", key() + service), 422, "account-closed")
        assertProblem(api.put("/admin/accounts/$accountId/status", """{"status":"ACTIVE"}"""), 422, "account-closed")
        assertThat(entryCount(accountId)).isZero()
    }

    @Test
    fun `after closing, the customer opens a new account with 201, and the closed one stays readable`() {
        val (customerId, accountId) = openAccount()
        close(accountId, asCustomer(customerId))

        val reopened = api.openAccount(customerId)

        assertThat(reopened.status).describedAs(reopened.rawBody).isEqualTo(201)
        assertThat(reopened.body!!.get("id").asString()).isNotEqualTo(accountId)
        assertThat(reopened.body.get("status").asString()).isEqualTo("ACTIVE")
        assertThat(api.get("/accounts/$accountId", asCustomer(customerId)).status).isEqualTo(200)
        assertProblem(api.openAccount(customerId), 409, "customer-already-has-account")
    }

    private fun close(
        accountId: String,
        headers: Map<String, String>,
    ) = api.send(org.springframework.http.HttpMethod.POST, "/accounts/$accountId/close", headers = headers)

    private fun openAccount(type: String? = null): Pair<UUID, String> {
        val customerId = UUID.randomUUID()
        val response = api.openAccount(customerId)
        assertThat(response.status).describedAs(response.rawBody).isEqualTo(201)
        val accountId = response.body!!.get("id").asString()
        if (type != null) assertThat(api.put("/accounts/$accountId/type", """{"type":"$type"}""").status).isEqualTo(200)
        return customerId to accountId
    }

    private fun statusOf(accountId: String) =
        api
            .get("/accounts/$accountId")
            .body!!
            .get("status")
            .asString()

    private fun key() = mapOf("Idempotency-Key" to UUID.randomUUID().toString())

    private fun entryCount(accountId: String): Long =
        ownerJdbc
            .sql("select count(*) from ledger_entry where account_id = :id")
            .param("id", UUID.fromString(accountId))
            .query(Long::class.java)
            .single()

    private fun outboxRows(
        accountId: String,
        eventType: String,
    ) = ownerJdbc
        .sql("select payload::text as payload, correlation_id from outbox where aggregate_id = :id and event_type = :type")
        .param("id", accountId)
        .param("type", eventType)
        .query { rs, _ -> mapper.readTree(rs.getString("payload")) to rs.getString("correlation_id") }
        .list()
}
