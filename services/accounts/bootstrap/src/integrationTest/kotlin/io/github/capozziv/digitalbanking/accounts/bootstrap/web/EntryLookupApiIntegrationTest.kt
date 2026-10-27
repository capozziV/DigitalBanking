package io.github.capozziv.digitalbanking.accounts.bootstrap.web

import io.github.capozziv.digitalbanking.accounts.bootstrap.AccountsIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import java.util.UUID

class EntryLookupApiIntegrationTest : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }

    @Test
    fun `an existing credit is 200 with the same body as the request that recorded it`() {
        val accountId = openAccount()
        val key = "transfer-${UUID.randomUUID()}-credit"
        val recorded = api.post("/accounts/$accountId/credits", """{"amount":1000}""", key(key))
        assertThat(recorded.status).isEqualTo(201)

        val found = api.get("/accounts/$accountId/entries?idempotencyKey=$key")

        assertThat(found.status).describedAs(found.rawBody).isEqualTo(200)
        assertThat(found.body).isEqualTo(recorded.body)
        assertThat(found.body!!.get("kind").asString()).isEqualTo("CREDIT")
        assertThat(found.body.get("sequence").asLong()).isEqualTo(1)
        assertThat(found.body.get("amount").asLong()).isEqualTo(1000)
        assertThat(found.body.get("balanceAfter").asLong()).isEqualTo(1000)
    }

    @Test
    fun `an existing debit is found with its balanceAfter`() {
        val accountId = openAccount()
        api.post("/accounts/$accountId/credits", """{"amount":1000}""", key(UUID.randomUUID().toString()))
        val key = "transfer-${UUID.randomUUID()}-debit"
        val recorded = api.post("/accounts/$accountId/debits", """{"amount":300}""", key(key))

        val found = api.get("/accounts/$accountId/entries?idempotencyKey=$key")

        assertThat(found.status).isEqualTo(200)
        assertThat(found.body).isEqualTo(recorded.body)
        assertThat(found.body!!.get("kind").asString()).isEqualTo("DEBIT")
        assertThat(found.body.get("balanceAfter").asLong()).isEqualTo(700)
    }

    @Test
    fun `a key recorded on another account is 404 entry-not-found`() {
        val accountId = openAccount()
        val otherAccountId = openAccount()
        val key = UUID.randomUUID().toString()
        api.post("/accounts/$otherAccountId/credits", """{"amount":1000}""", key(key))

        assertProblem(api.get("/accounts/$accountId/entries?idempotencyKey=$key"), 404, "entry-not-found")
    }

    @Test
    fun `a lookup of an unused key records nothing`() {
        val accountId = openAccount()

        assertProblem(api.get("/accounts/$accountId/entries?idempotencyKey=never-used"), 404, "entry-not-found")
        val account = api.get("/accounts/$accountId")
        assertThat(account.body!!.get("balance").asLong()).isEqualTo(0)
    }

    @Test
    fun `an unknown account is 404 account-not-found`() {
        assertProblem(api.get("/accounts/${UUID.randomUUID()}/entries?idempotencyKey=any"), 404, "account-not-found")
    }

    @Test
    fun `a missing idempotencyKey is 400 validation-error`() {
        val response = api.get("/accounts/${openAccount()}/entries")

        assertProblem(response, 400, "validation-error")
        assertThat(response.errorFields()).containsExactly("idempotencyKey")
    }

    @Test
    fun `an empty or blank idempotencyKey is 400 validation-error`() {
        val accountId = openAccount()

        assertProblem(api.get("/accounts/$accountId/entries?idempotencyKey="), 400, "validation-error")
        assertProblem(api.get("/accounts/$accountId/entries?idempotencyKey= "), 400, "validation-error")
    }

    @Test
    fun `an idempotencyKey of 256 characters is 400 and of 255 is a valid lookup`() {
        val accountId = openAccount()

        assertProblem(api.get("/accounts/$accountId/entries?idempotencyKey=${"k".repeat(256)}"), 400, "validation-error")
        assertProblem(api.get("/accounts/$accountId/entries?idempotencyKey=${"k".repeat(255)}"), 404, "entry-not-found")
    }

    @Test
    fun `an id that is not a UUID is 400 validation-error`() {
        assertProblem(api.get("/accounts/not-a-uuid/entries?idempotencyKey=any"), 400, "validation-error")
    }

    private fun key(value: String) = mapOf("Idempotency-Key" to value)

    private fun openAccount(): String {
        val response = api.post("/accounts", """{"customerId":"${UUID.randomUUID()}"}""", key(UUID.randomUUID().toString()))
        assertThat(response.status).isEqualTo(201)
        return response.body!!.get("id").asString()
    }
}
