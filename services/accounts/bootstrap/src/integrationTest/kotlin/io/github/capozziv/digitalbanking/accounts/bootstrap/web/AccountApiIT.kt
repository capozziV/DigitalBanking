package io.github.capozziv.digitalbanking.accounts.bootstrap.web

import io.github.capozziv.digitalbanking.accounts.bootstrap.AccountsIntegrationTest
import io.micrometer.core.instrument.MeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.test.web.server.LocalServerPort
import java.sql.DriverManager
import java.util.UUID

@ExtendWith(OutputCaptureExtension::class)
class AccountApiIT : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    private val api by lazy { ApiClient(port) }

    // --- POST /accounts ---

    @Test
    fun `opening an account returns 201 with a FREE account, zero balance and its location`() {
        val customerId = UUID.randomUUID()

        val response = api.post("/accounts", """{"customerId":"$customerId"}""", key(UUID.randomUUID().toString()))

        assertThat(response.status).isEqualTo(201)
        val body = response.body!!
        val id = body.get("id").asString()
        assertThat(UUID.fromString(id)).isNotNull()
        assertThat(body.get("customerId").asString()).isEqualTo(customerId.toString())
        assertThat(body.get("type").asString()).isEqualTo("FREE")
        assertThat(body.get("balance").asLong()).isEqualTo(0)
        assertThat(body.get("overdraftLimit").asLong()).isEqualTo(0)
        assertThat(body.get("availableBalance").asLong()).isEqualTo(0)
        assertThat(response.header("Location")).isEqualTo("/accounts/$id")
    }

    @Test
    fun `opening again with the same key returns 200 with the same account`() {
        val customerId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID().toString()
        val first = api.post("/accounts", """{"customerId":"$customerId"}""", key(idempotencyKey))

        val retry = api.post("/accounts", """{"customerId":"$customerId"}""", key(idempotencyKey))

        assertThat(retry.status).isEqualTo(200)
        assertThat(retry.body).isEqualTo(first.body)
    }

    @Test
    fun `opening a second account for the same customer with another key is 409 customer-already-has-account`() {
        val customerId = UUID.randomUUID()
        api.post("/accounts", """{"customerId":"$customerId"}""", key(UUID.randomUUID().toString()))

        val response = api.post("/accounts", """{"customerId":"$customerId"}""", key(UUID.randomUUID().toString()))

        assertProblem(response, 409, "customer-already-has-account")
    }

    @Test
    fun `opening with a key already used by another customer is 422 idempotency-key-reused`() {
        val idempotencyKey = UUID.randomUUID().toString()
        api.post("/accounts", """{"customerId":"${UUID.randomUUID()}"}""", key(idempotencyKey))

        val response = api.post("/accounts", """{"customerId":"${UUID.randomUUID()}"}""", key(idempotencyKey))

        assertProblem(response, 422, "idempotency-key-reused")
    }

    @Test
    fun `opening without Idempotency-Key is 400 missing-idempotency-key`() {
        val response = api.post("/accounts", """{"customerId":"${UUID.randomUUID()}"}""")

        assertProblem(response, 400, "missing-idempotency-key")
    }

    @Test
    fun `a blank Idempotency-Key is 400 validation-error`() {
        val response = api.post("/accounts", """{"customerId":"${UUID.randomUUID()}"}""", key("   "))

        assertProblem(response, 400, "validation-error")
        assertThat(response.errorFields()).containsExactly("Idempotency-Key")
    }

    @Test
    fun `an Idempotency-Key of 255 characters is accepted and one of 256 is 400 validation-error`() {
        val accepted = api.post("/accounts", """{"customerId":"${UUID.randomUUID()}"}""", key(uniqueKeyOfLength(255)))
        val tooLong = api.post("/accounts", """{"customerId":"${UUID.randomUUID()}"}""", key(uniqueKeyOfLength(256)))

        assertThat(accepted.status).isEqualTo(201)
        assertProblem(tooLong, 400, "validation-error")
        assertThat(tooLong.errorFields()).containsExactly("Idempotency-Key")
    }

    @Test
    fun `opening without customerId or with a customerId that is not a UUID is 400 validation-error`() {
        val missing = api.post("/accounts", """{}""", key(UUID.randomUUID().toString()))
        val notUuid = api.post("/accounts", """{"customerId":"abc"}""", key(UUID.randomUUID().toString()))

        assertProblem(missing, 400, "validation-error")
        assertThat(missing.errorFields()).containsExactly("customerId")
        assertProblem(notUuid, 400, "validation-error")
        assertThat(notUuid.errorFields()).containsExactly("customerId")
    }

    @Test
    fun `a malformed JSON body is 400 validation-error`() {
        val response = api.post("/accounts", """{"customerId":""", key(UUID.randomUUID().toString()))

        assertProblem(response, 400, "validation-error")
    }

    // --- GET /accounts/{id} ---

    @Test
    fun `get returns the balance, the overdraft limit of the type and the available balance`() {
        val accountId = openAccount()
        api.put("/accounts/$accountId/type", """{"type":"STANDARD"}""")
        credit(accountId, 1_000)
        debit(accountId, 1_500)

        val response = api.get("/accounts/$accountId")

        assertThat(response.status).isEqualTo(200)
        val body = response.body!!
        assertThat(body.get("id").asString()).isEqualTo(accountId)
        assertThat(body.get("type").asString()).isEqualTo("STANDARD")
        assertThat(body.get("balance").asLong()).isEqualTo(-500)
        assertThat(body.get("overdraftLimit").asLong()).isEqualTo(50_000)
        assertThat(body.get("availableBalance").asLong()).isEqualTo(49_500)
    }

    @Test
    fun `get of an unknown account is 404 account-not-found`() {
        assertProblem(api.get("/accounts/${UUID.randomUUID()}"), 404, "account-not-found")
    }

    @Test
    fun `an account id that is not a UUID is 400 validation-error`() {
        val response = api.get("/accounts/not-a-uuid")

        assertProblem(response, 400, "validation-error")
        assertThat(response.errorFields()).containsExactly("id")
    }

    // --- POST /accounts/{id}/credits ---

    @Test
    fun `credit returns 201 with the entry and a retry with the same key returns 200 with the same body`() {
        val accountId = openAccount()
        val idempotencyKey = UUID.randomUUID().toString()

        val created = api.post("/accounts/$accountId/credits", """{"amount":2500,"description":"salary"}""", key(idempotencyKey))
        val retry = api.post("/accounts/$accountId/credits", """{"amount":2500,"description":"salary"}""", key(idempotencyKey))

        assertThat(created.status).isEqualTo(201)
        val body = created.body!!
        assertThat(body.propertyNames()).containsExactlyInAnyOrder(
            "id",
            "accountId",
            "sequence",
            "kind",
            "amount",
            "balanceAfter",
            "description",
            "createdAt",
        )
        assertThat(body.get("accountId").asString()).isEqualTo(accountId)
        assertThat(body.get("sequence").asLong()).isEqualTo(1)
        assertThat(body.get("kind").asString()).isEqualTo("CREDIT")
        assertThat(body.get("amount").asLong()).isEqualTo(2_500)
        assertThat(body.get("balanceAfter").asLong()).isEqualTo(2_500)
        assertThat(body.get("description").asString()).isEqualTo("salary")
        assertThat(retry.status).isEqualTo(200)
        assertThat(retry.rawBody).isEqualTo(created.rawBody)
        assertThat(entryCount(accountId)).isEqualTo(1)
    }

    @Test
    fun `credit with a key already used for another amount is 422 idempotency-key-reused`() {
        val accountId = openAccount()
        val idempotencyKey = UUID.randomUUID().toString()
        api.post("/accounts/$accountId/credits", """{"amount":100}""", key(idempotencyKey))

        val response = api.post("/accounts/$accountId/credits", """{"amount":200}""", key(idempotencyKey))

        assertProblem(response, 422, "idempotency-key-reused")
    }

    @Test
    fun `debit with the key of a credit of the same account is 422 idempotency-key-reused`() {
        val accountId = openAccount()
        val idempotencyKey = UUID.randomUUID().toString()
        api.post("/accounts/$accountId/credits", """{"amount":100}""", key(idempotencyKey))

        val response = api.post("/accounts/$accountId/debits", """{"amount":100}""", key(idempotencyKey))

        assertProblem(response, 422, "idempotency-key-reused")
    }

    @Test
    fun `credit to an unknown account is 404 account-not-found`() {
        val response = api.post("/accounts/${UUID.randomUUID()}/credits", """{"amount":100}""", key(UUID.randomUUID().toString()))

        assertProblem(response, 404, "account-not-found")
    }

    @Test
    fun `credit without Idempotency-Key is 400 missing-idempotency-key`() {
        val response = api.post("/accounts/${openAccount()}/credits", """{"amount":100}""")

        assertProblem(response, 400, "missing-idempotency-key")
    }

    @Test
    fun `an amount that is missing, zero, negative, fractional or text is 400 validation-error`() {
        val accountId = openAccount()

        for (body in listOf("""{}""", """{"amount":0}""", """{"amount":-1}""", """{"amount":1.5}""", """{"amount":"ten"}""")) {
            val response = api.post("/accounts/$accountId/credits", body, key(UUID.randomUUID().toString()))

            assertProblem(response, 400, "validation-error")
            assertThat(response.errorFields()).describedAs(body).containsExactly("amount")
        }
        assertThat(entryCount(accountId)).isZero()
    }

    @Test
    fun `a description of 140 characters is accepted and one of 141 is 400 validation-error`() {
        val accountId = openAccount()

        val accepted =
            api.post(
                "/accounts/$accountId/credits",
                """{"amount":1,"description":"${"d".repeat(140)}"}""",
                key(UUID.randomUUID().toString()),
            )
        val tooLong =
            api.post(
                "/accounts/$accountId/credits",
                """{"amount":1,"description":"${"d".repeat(141)}"}""",
                key(UUID.randomUUID().toString()),
            )

        assertThat(accepted.status).isEqualTo(201)
        assertThat(accepted.body!!.get("description").asString()).hasSize(140)
        assertProblem(tooLong, 400, "validation-error")
        assertThat(tooLong.errorFields()).containsExactly("description")
    }

    @Test
    fun `an empty or blank description is recorded as absent`() {
        val accountId = openAccount()

        val empty = api.post("/accounts/$accountId/credits", """{"amount":1,"description":""}""", key(UUID.randomUUID().toString()))
        val blank = api.post("/accounts/$accountId/debits", """{"amount":1,"description":"   "}""", key(UUID.randomUUID().toString()))

        assertThat(empty.status).isEqualTo(201)
        assertThat(empty.body!!.get("description").isNull).isTrue()
        assertThat(blank.status).isEqualTo(201)
        assertThat(blank.body!!.get("description").isNull).isTrue()
    }

    @Test
    fun `a credit that would overflow the balance is 400 validation-error and records nothing`() {
        val accountId = openAccount()
        credit(accountId, Long.MAX_VALUE)

        val response = api.post("/accounts/$accountId/credits", """{"amount":1}""", key(UUID.randomUUID().toString()))

        assertProblem(response, 400, "validation-error")
        assertThat(response.errorFields()).containsExactly("amount")
        assertThat(entryCount(accountId)).isEqualTo(1)
    }

    @Test
    fun `credit while another transaction holds the account is 503 account-busy with Retry-After 1`() {
        val accountId = openAccount()

        val response =
            whileAccountIsLocked(accountId) {
                api.post("/accounts/$accountId/credits", """{"amount":100}""", key(UUID.randomUUID().toString()))
            }

        assertProblem(response, 503, "account-busy")
        assertThat(response.header("Retry-After")).isEqualTo("1")
        assertThat(entryCount(accountId)).isZero()
    }

    // --- POST /accounts/{id}/debits ---

    @Test
    fun `debit returns 201 with the entry and a retry with the same key returns 200 with the same body`() {
        val accountId = openAccount()
        credit(accountId, 1_000)
        val idempotencyKey = UUID.randomUUID().toString()

        val created = api.post("/accounts/$accountId/debits", """{"amount":400}""", key(idempotencyKey))
        val retry = api.post("/accounts/$accountId/debits", """{"amount":400}""", key(idempotencyKey))

        assertThat(created.status).isEqualTo(201)
        assertThat(created.body!!.get("kind").asString()).isEqualTo("DEBIT")
        assertThat(created.body.get("sequence").asLong()).isEqualTo(2)
        assertThat(created.body.get("balanceAfter").asLong()).isEqualTo(600)
        assertThat(created.body.get("description").isNull).isTrue()
        assertThat(retry.status).isEqualTo(200)
        assertThat(retry.rawBody).isEqualTo(created.rawBody)
        assertThat(entryCount(accountId)).isEqualTo(2)
    }

    @Test
    fun `debit equal to the available balance of a STANDARD account takes the balance down to the overdraft limit`() {
        val accountId = openAccount()
        api.put("/accounts/$accountId/type", """{"type":"STANDARD"}""")
        credit(accountId, 1_000)

        val response = api.post("/accounts/$accountId/debits", """{"amount":51000}""", key(UUID.randomUUID().toString()))

        assertThat(response.status).isEqualTo(201)
        assertThat(response.body!!.get("balanceAfter").asLong()).isEqualTo(-50_000)
    }

    @Test
    fun `debit above the available balance is 422 insufficient-available-balance, logged and counted`(output: CapturedOutput) {
        val accountId = openAccount()
        api.put("/accounts/$accountId/type", """{"type":"STANDARD"}""")
        credit(accountId, 1_000)
        val refusedBefore = refusedDebits()

        val response = api.post("/accounts/$accountId/debits", """{"amount":51001}""", key(UUID.randomUUID().toString()))

        assertProblem(response, 422, "insufficient-available-balance")
        assertThat(response.body!!.get("availableBalance").asLong()).isEqualTo(51_000)
        assertThat(refusedDebits()).isEqualTo(refusedBefore + 1)
        assertThat(entryCount(accountId)).isEqualTo(1)
        assertThat(output.out).containsPattern("INFO.*debit refused.*$accountId.*51000")
    }

    @Test
    fun `debit from an unknown account is 404 account-not-found`() {
        val response = api.post("/accounts/${UUID.randomUUID()}/debits", """{"amount":100}""", key(UUID.randomUUID().toString()))

        assertProblem(response, 404, "account-not-found")
    }

    @Test
    fun `debit without Idempotency-Key is 400 missing-idempotency-key`() {
        val response = api.post("/accounts/${openAccount()}/debits", """{"amount":100}""")

        assertProblem(response, 400, "missing-idempotency-key")
    }

    @Test
    fun `debit with an invalid amount is 400 validation-error`() {
        val response = api.post("/accounts/${openAccount()}/debits", """{"amount":0}""", key(UUID.randomUUID().toString()))

        assertProblem(response, 400, "validation-error")
        assertThat(response.errorFields()).containsExactly("amount")
    }

    @Test
    fun `debit while another transaction holds the account is 503 account-busy with Retry-After 1`() {
        val accountId = openAccount()
        credit(accountId, 1_000)

        val response =
            whileAccountIsLocked(accountId) {
                api.post("/accounts/$accountId/debits", """{"amount":100}""", key(UUID.randomUUID().toString()))
            }

        assertProblem(response, 503, "account-busy")
        assertThat(response.header("Retry-After")).isEqualTo("1")
    }

    // --- PUT /accounts/{id}/type ---

    @Test
    fun `changing the type returns 200 with the account and its new overdraft limit`() {
        val accountId = openAccount()

        val response = api.put("/accounts/$accountId/type", """{"type":"GOLD"}""")

        assertThat(response.status).isEqualTo(200)
        assertThat(response.body!!.get("id").asString()).isEqualTo(accountId)
        assertThat(response.body.get("type").asString()).isEqualTo("GOLD")
        assertThat(response.body.get("overdraftLimit").asLong()).isEqualTo(200_000)
        assertThat(response.body.get("availableBalance").asLong()).isEqualTo(200_000)
    }

    @Test
    fun `a type that is unknown, lowercase or missing in the body is 400 validation-error`() {
        val accountId = openAccount()

        for (body in listOf("""{"type":"gold"}""", """{"type":"PLATINUM"}""", """{}""")) {
            val response = api.put("/accounts/$accountId/type", body)

            assertProblem(response, 400, "validation-error")
            assertThat(response.errorFields()).describedAs(body).containsExactly("type")
        }
    }

    @Test
    fun `changing the type of an unknown account is 404 and of a non-UUID id is 400`() {
        assertProblem(api.put("/accounts/${UUID.randomUUID()}/type", """{"type":"GOLD"}"""), 404, "account-not-found")
        assertProblem(api.put("/accounts/xyz/type", """{"type":"GOLD"}"""), 400, "validation-error")
    }

    @Test
    fun `changing the type while another transaction holds the account is 503 account-busy`() {
        val accountId = openAccount()

        val response = whileAccountIsLocked(accountId) { api.put("/accounts/$accountId/type", """{"type":"GOLD"}""") }

        assertProblem(response, 503, "account-busy")
        assertThat(response.header("Retry-After")).isEqualTo("1")
    }

    // --- X-Correlation-Id ---

    @Test
    fun `the correlation id sent is returned and recorded in the outbox`() {
        val accountId = openAccount()
        val correlationId = UUID.randomUUID().toString()

        val response =
            api.post(
                "/accounts/$accountId/credits",
                """{"amount":100}""",
                key(UUID.randomUUID().toString()) + ("X-Correlation-Id" to correlationId),
            )

        assertThat(response.status).isEqualTo(201)
        assertThat(response.header("X-Correlation-Id")).isEqualTo(correlationId)
        assertThat(outboxCorrelationIds(accountId, "LedgerEntryRecorded")).containsExactly(correlationId)
    }

    @Test
    fun `without a correlation id one is generated, returned and recorded in the outbox`() {
        val customerId = UUID.randomUUID()

        val response = api.post("/accounts", """{"customerId":"$customerId"}""", key(UUID.randomUUID().toString()))

        val generated = response.header("X-Correlation-Id")
        assertThat(generated).isNotBlank()
        assertThat(UUID.fromString(generated)).isNotNull()
        assertThat(outboxCorrelationIds(response.body!!.get("id").asString(), "AccountOpened")).containsExactly(generated)
    }

    @Test
    fun `error responses also carry the correlation id`() {
        val correlationId = UUID.randomUUID().toString()

        val response = api.get("/accounts/${UUID.randomUUID()}", mapOf("X-Correlation-Id" to correlationId))

        assertThat(response.status).isEqualTo(404)
        assertThat(response.header("X-Correlation-Id")).isEqualTo(correlationId)
    }

    // --- helpers ---

    private fun key(value: String) = mapOf("Idempotency-Key" to value)

    private fun uniqueKeyOfLength(length: Int): String {
        val prefix = UUID.randomUUID().toString()
        return prefix + "k".repeat(length - prefix.length)
    }

    private fun openAccount(): String {
        val response = api.post("/accounts", """{"customerId":"${UUID.randomUUID()}"}""", key(UUID.randomUUID().toString()))
        assertThat(response.status).isEqualTo(201)
        return response.body!!.get("id").asString()
    }

    private fun credit(
        accountId: String,
        amount: Long,
    ) {
        val response = api.post("/accounts/$accountId/credits", """{"amount":$amount}""", key(UUID.randomUUID().toString()))
        assertThat(response.status).isEqualTo(201)
    }

    private fun debit(
        accountId: String,
        amount: Long,
    ) {
        val response = api.post("/accounts/$accountId/debits", """{"amount":$amount}""", key(UUID.randomUUID().toString()))
        assertThat(response.status).isEqualTo(201)
    }

    private fun refusedDebits(): Double = meterRegistry.find("accounts.debit.refused").counter()?.count() ?: 0.0

    private fun entryCount(accountId: String): Long =
        ownerJdbc
            .sql("select count(*) from ledger_entry where account_id = :id")
            .param("id", UUID.fromString(accountId))
            .query(Long::class.java)
            .single()

    private fun outboxCorrelationIds(
        accountId: String,
        eventType: String,
    ): List<String?> =
        ownerJdbc
            .sql("select correlation_id from outbox where aggregate_id = :id and event_type = :type")
            .param("id", accountId)
            .param("type", eventType)
            .query(String::class.java)
            .list()

    private fun <T> whileAccountIsLocked(
        accountId: String,
        block: () -> T,
    ): T =
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
            connection.autoCommit = false
            connection.prepareStatement("select id from account where id = ? for update").use {
                it.setObject(1, UUID.fromString(accountId))
                it.executeQuery().use { rs -> check(rs.next()) }
            }
            try {
                block()
            } finally {
                connection.rollback()
            }
        }
}
