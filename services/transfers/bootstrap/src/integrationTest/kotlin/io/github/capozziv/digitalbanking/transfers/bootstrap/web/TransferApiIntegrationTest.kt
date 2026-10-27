package io.github.capozziv.digitalbanking.transfers.bootstrap.web

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.web.server.LocalServerPort
import java.util.UUID

/** The transfers API against a WireMock that stands in for `accounts`: one test per row of the spec's response table. */
class TransferApiIntegrationTest : TransfersIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }

    private val source = UUID.randomUUID()
    private val destination = UUID.randomUUID()

    @BeforeEach
    fun resetAccounts() {
        accountsServer.resetAll()
        stubAccountExists(source)
        stubAccountExists(destination)
    }

    // --- POST /transfers: the response table ---

    @Test
    fun `a completed transfer is 201 with its location, and the repetition is 200 without calling accounts again`() {
        stubEntry("/accounts/$source/debits", 201)
        stubEntry("/accounts/$destination/credits", 201)
        val key = key()

        val first = api.post("/transfers", transferJson(amount = "1500", description = "rent"), key)

        assertThat(first.status).describedAs(first.rawBody).isEqualTo(201)
        val body = first.body!!
        val id = body.get("id").asString()
        assertThat(first.header("Location")).isEqualTo("/transfers/$id")
        assertThat(body.get("sourceAccountId").asString()).isEqualTo(source.toString())
        assertThat(body.get("destinationAccountId").asString()).isEqualTo(destination.toString())
        assertThat(body.get("amount").asLong()).isEqualTo(1500)
        assertThat(body.get("description").asString()).isEqualTo("rent")
        assertThat(body.get("status").asString()).isEqualTo("COMPLETED")
        assertThat(body.get("failureReason").isNull).isTrue()
        assertThat(body.get("createdAt").asString()).isNotBlank()
        assertThat(body.get("updatedAt").asString()).isNotBlank()
        accountsServer.verify(
            1,
            postRequestedFor(urlEqualTo("/accounts/$source/debits")).withHeader("Idempotency-Key", equalTo("transfer-$id-debit")),
        )
        accountsServer.verify(
            1,
            postRequestedFor(urlEqualTo("/accounts/$destination/credits")).withHeader("Idempotency-Key", equalTo("transfer-$id-credit")),
        )

        val repeated = api.post("/transfers", transferJson(amount = "1500", description = "other words"), key)

        assertThat(repeated.status).describedAs(repeated.rawBody).isEqualTo(200)
        assertThat(repeated.body!!.get("id").asString()).isEqualTo(id)
        assertThat(repeated.body.get("status").asString()).isEqualTo("COMPLETED")
        accountsServer.verify(1, postRequestedFor(urlEqualTo("/accounts/$source/debits")))
        accountsServer.verify(1, postRequestedFor(urlEqualTo("/accounts/$destination/credits")))
    }

    @Test
    fun `a transfer without description has a null description`() {
        stubEntry("/accounts/$source/debits", 201)
        stubEntry("/accounts/$destination/credits", 201)

        val response = api.post("/transfers", transferJson(amount = "10"), key())

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(201)
        assertThat(response.body!!.get("description").isNull).isTrue()
    }

    @Test
    fun `a pending transfer is 202, and the repetition is 200 with the current status`() {
        stubEntry("/accounts/$source/debits", 201)
        accountsServer.stubFor(post(urlEqualTo("/accounts/$destination/credits")).willReturn(aResponse().withStatus(503)))
        val key = key()

        val first = api.post("/transfers", transferJson(amount = "700"), key)

        assertThat(first.status).describedAs(first.rawBody).isEqualTo(202)
        assertThat(first.body!!.get("status").asString()).isEqualTo("PENDING")
        val id = first.body.get("id").asString()

        val repeated = api.post("/transfers", transferJson(amount = "700"), key)

        assertThat(repeated.status).describedAs(repeated.rawBody).isEqualTo(200)
        assertThat(repeated.body!!.get("id").asString()).isEqualTo(id)
        assertThat(repeated.body.get("status").asString()).isEqualTo("PENDING")
    }

    @Test
    fun `a compensated transfer is 200 on the first answer and on the repetition`() {
        stubEntry("/accounts/$source/debits", 201)
        accountsServer.stubFor(post(urlEqualTo("/accounts/$destination/credits")).willReturn(problem(404, "account-not-found")))
        stubEntry("/accounts/$source/credits", 201)
        val key = key()

        val first = api.post("/transfers", transferJson(amount = "300"), key)

        assertThat(first.status).describedAs(first.rawBody).isEqualTo(200)
        assertThat(first.body!!.get("status").asString()).isEqualTo("COMPENSATED")
        val id = first.body.get("id").asString()
        accountsServer.verify(
            1,
            postRequestedFor(urlEqualTo("/accounts/$source/credits")).withHeader("Idempotency-Key", equalTo("transfer-$id-compensation")),
        )

        val repeated = api.post("/transfers", transferJson(amount = "300"), key)

        assertThat(repeated.status).describedAs(repeated.rawBody).isEqualTo(200)
        assertThat(repeated.body!!.get("status").asString()).isEqualTo("COMPENSATED")
    }

    @Test
    fun `a debit refused for lack of balance is 422 insufficient-available-balance with the transfer id, also on the repetition`() {
        accountsServer.stubFor(
            post(urlEqualTo("/accounts/$source/debits")).willReturn(problem(422, "insufficient-available-balance")),
        )
        val key = key()

        val first = api.post("/transfers", transferJson(amount = "999999"), key)

        assertProblem(first, 422, "insufficient-available-balance")
        val transferId = first.body!!.get("transferId").asString()
        assertThat(UUID.fromString(transferId)).isNotNull()

        val repeated = api.post("/transfers", transferJson(amount = "999999"), key)

        assertProblem(repeated, 422, "insufficient-available-balance")
        assertThat(repeated.body!!.get("transferId").asString()).isEqualTo(transferId)
        accountsServer.verify(1, postRequestedFor(urlEqualTo("/accounts/$source/debits")))
        val stored = api.get("/transfers/$transferId")
        assertThat(stored.body!!.get("status").asString()).isEqualTo("FAILED")
    }

    @Test
    fun `a transfer that failed for another reason is 422 transfer-failed with the reason, also on the repetition`() {
        accountsServer.stubFor(post(urlEqualTo("/accounts/$source/debits")).willReturn(problem(404, "account-not-found")))
        val key = key()

        val first = api.post("/transfers", transferJson(amount = "50"), key)

        assertProblem(first, 422, "transfer-failed")
        assertThat(first.body!!.get("reason").asString()).isEqualTo("account-not-found")

        val repeated = api.post("/transfers", transferJson(amount = "50"), key)

        assertProblem(repeated, 422, "transfer-failed")
        assertThat(repeated.body!!.get("reason").asString()).isEqualTo("account-not-found")
    }

    // --- POST /transfers: refusals before anything is stored ---

    @Test
    fun `the same account is 422 same-account-transfer`() {
        val response = api.post("/transfers", transferJson(destinationId = source.toString(), amount = "10"), key())

        assertProblem(response, 422, "same-account-transfer")
        assertThat(accountsServer.allServeEvents).isEmpty()
    }

    @Test
    fun `the same account written with different letter case is still 422 same-account-transfer`() {
        val lower = source.toString()
        val response =
            api.post(
                "/transfers",
                transferJson(sourceId = lower.uppercase(), destinationId = lower, amount = "10"),
                key(),
            )

        assertProblem(response, 422, "same-account-transfer")
        assertThat(accountsServer.allServeEvents).isEmpty()
    }

    @Test
    fun `an unknown account is 422 account-not-found naming it, and nothing is stored or debited`() {
        val missing = UUID.randomUUID()
        accountsServer.stubFor(get(urlEqualTo("/accounts/$missing")).willReturn(problem(404, "account-not-found")))
        val before = transferCount()

        val response = api.post("/transfers", transferJson(destinationId = missing.toString(), amount = "10"), key())

        assertProblem(response, 422, "account-not-found")
        assertThat(response.body!!.get("accountId").asString()).isEqualTo(missing.toString())
        assertThat(transferCount()).isEqualTo(before)
        accountsServer.verify(0, postRequestedFor(urlPathMatching("/accounts/.*")))
    }

    @Test
    fun `accounts being unavailable is 503 accounts-unavailable with Retry-After, and nothing is stored`() {
        accountsServer.stubFor(get(urlEqualTo("/accounts/$destination")).willReturn(aResponse().withStatus(503)))
        val before = transferCount()

        val response = api.post("/transfers", transferJson(amount = "10"), key())

        assertProblem(response, 503, "accounts-unavailable")
        assertThat(response.header("Retry-After")).isEqualTo("1")
        assertThat(transferCount()).isEqualTo(before)
    }

    @Test
    fun `the same key with another amount is 422 idempotency-key-reused`() {
        stubEntry("/accounts/$source/debits", 201)
        stubEntry("/accounts/$destination/credits", 201)
        val key = key()
        assertThat(api.post("/transfers", transferJson(amount = "100"), key).status).isEqualTo(201)

        val response = api.post("/transfers", transferJson(amount = "101"), key)

        assertProblem(response, 422, "idempotency-key-reused")
    }

    // --- POST /transfers: validation ---

    @Test
    fun `a missing Idempotency-Key is 400 missing-idempotency-key, even when the body is invalid`() {
        assertProblem(api.post("/transfers", transferJson(amount = "10")), 400, "missing-idempotency-key")
        assertProblem(api.post("/transfers", """{"amount":-1}"""), 400, "missing-idempotency-key")
    }

    @Test
    fun `an Idempotency-Key that is blank or longer than 255 characters is 400 validation-error`() {
        for (invalid in listOf(" ", "k".repeat(256))) {
            val response = api.post("/transfers", transferJson(amount = "10"), mapOf("Idempotency-Key" to invalid))

            assertProblem(response, 400, "validation-error")
            assertThat(response.errorFields()).containsExactly("Idempotency-Key")
        }
        assertThat(accountsServer.allServeEvents).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = ["0", "-1", "10.5", "9223372036854775808", "null"])
    fun `an invalid amount is 400 validation-error and accounts is never called`(amount: String) {
        val response = api.post("/transfers", transferJson(amount = amount), key())

        assertProblem(response, 400, "validation-error")
        assertThat(accountsServer.allServeEvents).isEmpty()
    }

    @Test
    fun `an absent amount, a bad account id, a long description and control characters are 400 validation-error`() {
        val key = key()
        val cases =
            listOf(
                """{"sourceAccountId":"$source","destinationAccountId":"$destination"}""" to "amount",
                """{"destinationAccountId":"$destination","amount":10}""" to "sourceAccountId",
                """{"sourceAccountId":"not-a-uuid","destinationAccountId":"$destination","amount":10}""" to "sourceAccountId",
                transferJson(amount = "10", description = "d".repeat(141)) to "description",
                transferJson(amount = "10", description = "line\\nbreak") to "description",
            )

        for ((body, field) in cases) {
            val response = api.post("/transfers", body, key)

            assertProblem(response, 400, "validation-error")
            assertThat(response.errorFields()).describedAs(body).contains(field)
        }
        assertThat(api.post("/transfers", "{not json", key).status).isEqualTo(400)
        assertThat(accountsServer.allServeEvents).isEmpty()
    }

    @Test
    fun `a description of exactly 140 characters is accepted and 141 is 400 validation-error`() {
        stubEntry("/accounts/$source/debits", 201)
        stubEntry("/accounts/$destination/credits", 201)
        val exact = "d".repeat(140)

        val accepted = api.post("/transfers", transferJson(amount = "10", description = exact), key())

        assertThat(accepted.status).describedAs(accepted.rawBody).isEqualTo(201)
        assertThat(accepted.body!!.get("description").asString()).isEqualTo(exact)

        val refused = api.post("/transfers", transferJson(amount = "10", description = "d".repeat(141)), key())

        assertProblem(refused, 400, "validation-error")
        assertThat(refused.errorFields()).contains("description")
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   "])
    fun `a blank description is stored as null`(description: String) {
        stubEntry("/accounts/$source/debits", 201)
        stubEntry("/accounts/$destination/credits", 201)

        val response = api.post("/transfers", transferJson(amount = "10", description = description), key())

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(201)
        assertThat(response.body!!.get("description").isNull).isTrue()
    }

    // --- GET /transfers/{id} ---

    @Test
    fun `get returns the transfer`() {
        stubEntry("/accounts/$source/debits", 201)
        stubEntry("/accounts/$destination/credits", 201)
        val created = api.post("/transfers", transferJson(amount = "42"), key())
        val id = created.body!!.get("id").asString()

        val response = api.get("/transfers/$id")

        assertThat(response.status).isEqualTo(200)
        assertThat(response.body!!.get("id").asString()).isEqualTo(id)
        assertThat(response.body.get("amount").asLong()).isEqualTo(42)
        assertThat(response.body.get("status").asString()).isEqualTo("COMPLETED")
    }

    @Test
    fun `get of an unknown id is 404 transfer-not-found`() {
        assertProblem(api.get("/transfers/${UUID.randomUUID()}"), 404, "transfer-not-found")
    }

    @Test
    fun `get of an id that is not a UUID is 400 validation-error`() {
        val response = api.get("/transfers/abc")

        assertProblem(response, 400, "validation-error")
    }

    // --- correlation id ---

    @Test
    fun `the X-Correlation-Id received reaches accounts and comes back in the response`() {
        stubEntry("/accounts/$source/debits", 201)
        stubEntry("/accounts/$destination/credits", 201)

        val response = api.post("/transfers", transferJson(amount = "10"), key() + mapOf("X-Correlation-Id" to "corr-abc-123"))

        assertThat(response.status).isEqualTo(201)
        assertThat(response.header("X-Correlation-Id")).isEqualTo("corr-abc-123")
        accountsServer.verify(getRequestedFor(urlEqualTo("/accounts/$source")).withHeader("X-Correlation-Id", equalTo("corr-abc-123")))
        accountsServer.verify(
            postRequestedFor(urlEqualTo("/accounts/$source/debits")).withHeader("X-Correlation-Id", equalTo("corr-abc-123")),
        )
        accountsServer.verify(
            postRequestedFor(urlEqualTo("/accounts/$destination/credits")).withHeader("X-Correlation-Id", equalTo("corr-abc-123")),
        )
    }

    @Test
    fun `a request without X-Correlation-Id gets a generated one`() {
        val response = api.get("/transfers/${UUID.randomUUID()}")

        assertThat(response.header("X-Correlation-Id")).isNotBlank()
    }

    // --- helpers ---

    private fun key(value: String = UUID.randomUUID().toString()) = mapOf("Idempotency-Key" to value)

    private fun transferJson(
        sourceId: String = source.toString(),
        destinationId: String = destination.toString(),
        amount: String,
        description: String? = null,
    ): String {
        val descriptionField = description?.let { ""","description":"$it"""" } ?: ""
        return """{"sourceAccountId":"$sourceId","destinationAccountId":"$destinationId","amount":$amount$descriptionField}"""
    }

    private fun transferCount(): Int = ownerJdbc.sql("select count(*) from transfer").query(Int::class.java).single()

    private fun stubAccountExists(id: UUID): StubMapping =
        accountsServer.stubFor(
            get(urlEqualTo("/accounts/$id")).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """{"id":"$id","customerId":"${UUID.randomUUID()}","type":"FREE","balance":0,""" +
                            """"overdraftLimit":0,"availableBalance":0}""",
                    ),
            ),
        )

    private fun stubEntry(
        path: String,
        status: Int,
    ): StubMapping =
        accountsServer.stubFor(
            post(urlEqualTo(path)).willReturn(
                aResponse()
                    .withStatus(status)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """{"id":"${UUID.randomUUID()}","accountId":"${UUID.randomUUID()}","sequence":1,"kind":"DEBIT","amount":1,""" +
                            """"balanceAfter":0,"createdAt":"2026-10-27T10:00:00Z"}""",
                    ),
            ),
        )

    private fun problem(
        status: Int,
        slug: String,
    ) = aResponse()
        .withStatus(status)
        .withHeader("Content-Type", "application/problem+json")
        .withBody("""{"type":"/problems/$slug","title":"t","status":$status}""")
}
