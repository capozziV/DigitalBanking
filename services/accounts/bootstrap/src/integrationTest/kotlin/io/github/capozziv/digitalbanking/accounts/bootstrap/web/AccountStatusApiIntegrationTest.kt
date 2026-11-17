package io.github.capozziv.digitalbanking.accounts.bootstrap.web

import io.github.capozziv.digitalbanking.accounts.bootstrap.AccountsIntegrationTest
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

/** `PUT /admin/accounts/{id}/status`: blocking and unblocking, and what a blocked account still does. */
class AccountStatusApiIntegrationTest : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }
    private val service = bearer(JwtTestSupport.serviceToken())
    private val mapper = JsonMapper.builder().build()

    @Test
    fun `an admin blocks an account, which is read back as BLOCKED, with one AccountBlocked event`() {
        val (customerId, accountId) = openAccount()

        val response = api.put("/admin/accounts/$accountId/status", """{"status":"BLOCKED"}""")

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("status").asString()).isEqualTo("BLOCKED")
        assertThat(
            api
                .get("/accounts/$accountId")
                .body!!
                .get("status")
                .asString(),
        ).isEqualTo("BLOCKED")
        val events = outboxPayloads(accountId, "AccountBlocked")
        assertThat(events).hasSize(1)
        assertThat(events.single().get("accountId").asString()).isEqualTo(accountId)
        assertThat(events.single().get("customerId").asString()).isEqualTo(customerId.toString())
        assertThat(events.single().get("occurredAt").asString()).isNotBlank()
    }

    @Test
    fun `an admin unblocks a blocked account with one AccountUnblocked event, and the same status again adds nothing`() {
        val (_, accountId) = openAccount()
        api.put("/admin/accounts/$accountId/status", """{"status":"BLOCKED"}""")

        val unblocked = api.put("/admin/accounts/$accountId/status", """{"status":"ACTIVE"}""")
        val again = api.put("/admin/accounts/$accountId/status", """{"status":"ACTIVE"}""")

        assertThat(unblocked.body!!.get("status").asString()).isEqualTo("ACTIVE")
        assertThat(again.status).isEqualTo(200)
        assertThat(outboxPayloads(accountId, "AccountUnblocked")).hasSize(1)
        assertThat(outboxPayloads(accountId, "AccountBlocked")).hasSize(1)
    }

    @Test
    fun `a customer, even the owner, and a service get 403 and the account stays active`() {
        val (customerId, accountId) = openAccount()

        for (token in listOf(JwtTestSupport.customerToken(customerId), JwtTestSupport.serviceToken())) {
            assertProblem(api.put("/admin/accounts/$accountId/status", """{"status":"BLOCKED"}""", bearer(token)), 403, "forbidden")
        }
        assertThat(
            api
                .get("/accounts/$accountId")
                .body!!
                .get("status")
                .asString(),
        ).isEqualTo("ACTIVE")
    }

    @Test
    fun `CLOSED, an unknown or a missing status is 400 validation-error`() {
        val (_, accountId) = openAccount()

        for (body in listOf("""{"status":"CLOSED"}""", """{"status":"FROZEN"}""", "{}")) {
            val response = api.put("/admin/accounts/$accountId/status", body)

            assertProblem(response, 400, "validation-error")
            assertThat(response.errorFields()).containsExactly("status")
        }
    }

    @Test
    fun `an unknown account is 404 account-not-found`() {
        assertProblem(api.put("/admin/accounts/${UUID.randomUUID()}/status", """{"status":"BLOCKED"}"""), 404, "account-not-found")
    }

    @Test
    fun `a blocked account refuses a debit with 422 account-blocked and records nothing, but takes a credit`() {
        val (_, accountId) = openAccount()
        assertThat(api.post("/accounts/$accountId/credits", """{"amount":1000}""", key() + service).status).isEqualTo(201)
        api.put("/admin/accounts/$accountId/status", """{"status":"BLOCKED"}""")

        val debit = api.post("/accounts/$accountId/debits", """{"amount":100}""", key() + service)
        val credit = api.post("/accounts/$accountId/credits", """{"amount":100}""", key() + service)

        assertProblem(debit, 422, "account-blocked")
        assertThat(credit.status).describedAs(credit.rawBody).isEqualTo(201)
        assertThat(
            api
                .get("/accounts/$accountId")
                .body!!
                .get("balance")
                .asLong(),
        ).isEqualTo(1100)
    }

    @Test
    fun `a customer whose account is blocked cannot open another, 409 customer-already-has-account`() {
        val (customerId, accountId) = openAccount()
        api.put("/admin/accounts/$accountId/status", """{"status":"BLOCKED"}""")

        assertProblem(api.openAccount(customerId), 409, "customer-already-has-account")
    }

    private fun openAccount(customerId: UUID = UUID.randomUUID()): Pair<UUID, String> {
        val response = api.openAccount(customerId)
        assertThat(response.status).describedAs(response.rawBody).isEqualTo(201)
        assertThat(response.body!!.get("status").asString()).isEqualTo("ACTIVE")
        return customerId to response.body.get("id").asString()
    }

    private fun key() = mapOf("Idempotency-Key" to UUID.randomUUID().toString())

    private fun outboxPayloads(
        accountId: String,
        eventType: String,
    ) = ownerJdbc
        .sql("select payload::text from outbox where aggregate_id = :id and event_type = :type order by position")
        .param("id", accountId)
        .param("type", eventType)
        .query(String::class.java)
        .list()
        .map { mapper.readTree(it) }
}
