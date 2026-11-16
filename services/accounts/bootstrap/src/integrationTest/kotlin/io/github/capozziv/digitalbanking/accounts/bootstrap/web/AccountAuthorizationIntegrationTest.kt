package io.github.capozziv.digitalbanking.accounts.bootstrap.web

import io.github.capozziv.digitalbanking.accounts.bootstrap.AccountsIntegrationTest
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.time.Duration
import java.util.UUID

/** Who may call each route, and the owner rule: a customer sees only its own account, others get 404. */
class AccountAuthorizationIntegrationTest : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    private val api by lazy { ApiClient(port) }
    private val anonymous by lazy { ApiClient(port, defaultToken = null) }

    private val owner = UUID.randomUUID()
    private val stranger = UUID.randomUUID()

    // --- 401 ---

    @Test
    fun `without a token, or with an expired one or one of another issuer, every route is 401 unauthorized`() {
        val accountId = openAccountOf(owner)
        val expired = JwtTestSupport.token(owner.toString(), expiresIn = Duration.ofMinutes(-5))
        val otherIssuer = JwtTestSupport.token(owner.toString(), issuer = "http://other.test")

        for (headers in listOf(emptyMap(), bearer(expired), bearer(otherIssuer), bearer("not-a-jwt"))) {
            assertProblem(anonymous.send(HttpMethod.POST, "/accounts", headers = headers + key()), 401, "unauthorized")
            assertProblem(anonymous.get("/accounts/$accountId", headers), 401, "unauthorized")
            assertProblem(anonymous.get("/accounts/$accountId/entries?idempotencyKey=k", headers), 401, "unauthorized")
            assertProblem(anonymous.post("/accounts/$accountId/credits", """{"amount":1}""", headers + key()), 401, "unauthorized")
            assertProblem(anonymous.post("/accounts/$accountId/debits", """{"amount":1}""", headers + key()), 401, "unauthorized")
            assertProblem(anonymous.put("/accounts/$accountId/type", """{"type":"GOLD"}""", headers), 401, "unauthorized")
            assertProblem(anonymous.get("/admin/account-types", headers), 401, "unauthorized")
        }
    }

    // --- POST /accounts ---

    @Test
    fun `a customer opens its own account, whatever customerId the body says`() {
        val response =
            api.post("/accounts", """{"customerId":"$stranger"}""", key() + asCustomer(owner))

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(201)
        assertThat(response.body!!.get("customerId").asString()).isEqualTo(owner.toString())
    }

    @Test
    fun `an admin who is also a customer opens an account for itself`() {
        val adminId = UUID.randomUUID()

        val response = api.openAccount(token = JwtTestSupport.adminToken(adminId))

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(201)
        assertThat(response.body!!.get("customerId").asString()).isEqualTo(adminId.toString())
    }

    @Test
    fun `a service cannot open an account`() {
        assertProblem(api.openAccount(token = JwtTestSupport.serviceToken()), 403, "forbidden")
    }

    // --- GET /accounts/{id} ---

    @Test
    fun `the owner reads its account`() {
        val accountId = openAccountOf(owner)

        val response = api.get("/accounts/$accountId", asCustomer(owner))

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("customerId").asString()).isEqualTo(owner.toString())
    }

    @Test
    fun `the account of another customer is 404 account-not-found, the same as an unknown account`() {
        val accountId = openAccountOf(owner)

        val response = api.get("/accounts/$accountId", asCustomer(stranger))

        assertProblem(response, 404, "account-not-found")
        assertThat(response.body!!.get("detail").asString()).isEqualTo("account $accountId not found")
    }

    @Test
    fun `an admin reads the account of any customer`() {
        val accountId = openAccountOf(owner)

        val response = api.get("/accounts/$accountId", bearer(JwtTestSupport.adminToken(UUID.randomUUID())))

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("customerId").asString()).isEqualTo(owner.toString())
    }

    @Test
    fun `a service reads any account and learns its customer`() {
        val accountId = openAccountOf(owner)

        val response = api.get("/accounts/$accountId", bearer(JwtTestSupport.serviceToken()))

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("customerId").asString()).isEqualTo(owner.toString())
    }

    // --- GET /accounts/{id}/entries ---

    @Test
    fun `the entry lookup follows the owner rule, and a service finds any entry`() {
        val accountId = openAccountOf(owner)
        val creditKey = UUID.randomUUID().toString()
        val credit = api.post("/accounts/$accountId/credits", """{"amount":100}""", key(creditKey))
        assertThat(credit.status).isEqualTo(201)
        val path = "/accounts/$accountId/entries?idempotencyKey=$creditKey"

        assertThat(api.get(path, asCustomer(owner)).status).isEqualTo(200)
        assertThat(api.get(path, bearer(JwtTestSupport.serviceToken())).status).isEqualTo(200)
        assertThat(api.get(path, bearer(JwtTestSupport.adminToken(UUID.randomUUID()))).status).isEqualTo(200)
        assertProblem(api.get(path, asCustomer(stranger)), 404, "account-not-found")
    }

    // --- credits and debits ---

    @Test
    fun `a customer token is 403 forbidden on credits and debits, even on its own account, and records nothing`() {
        val accountId = openAccountOf(owner)

        for (path in listOf("credits", "debits")) {
            val response = api.post("/accounts/$accountId/$path", """{"amount":100}""", key() + asCustomer(owner))

            assertProblem(response, 403, "forbidden")
        }
        assertThat(entryCount(accountId)).isZero()
    }

    @Test
    fun `a service token records a debit and a credit`() {
        val accountId = openAccountOf(owner)
        val service = bearer(JwtTestSupport.serviceToken())

        val credit = api.post("/accounts/$accountId/credits", """{"amount":1000}""", key() + service)
        val debit = api.post("/accounts/$accountId/debits", """{"amount":400}""", key() + service)

        assertThat(credit.status).describedAs(credit.rawBody).isEqualTo(201)
        assertThat(debit.status).describedAs(debit.rawBody).isEqualTo(201)
        assertThat(debit.body!!.get("balanceAfter").asLong()).isEqualTo(600)
    }

    @Test
    fun `an admin records manual credits and debits on any account`() {
        val accountId = openAccountOf(owner)

        val credit = api.post("/accounts/$accountId/credits", """{"amount":1000}""", key())
        val debit = api.post("/accounts/$accountId/debits", """{"amount":400}""", key())

        assertThat(credit.status).isEqualTo(201)
        assertThat(debit.status).isEqualTo(201)
    }

    // --- admin routes ---

    @Test
    fun `changing the type and the admin routes are only for admins`() {
        val accountId = openAccountOf(owner)

        for (token in listOf(JwtTestSupport.customerToken(owner), JwtTestSupport.serviceToken())) {
            assertProblem(api.put("/accounts/$accountId/type", """{"type":"GOLD"}""", bearer(token)), 403, "forbidden")
            assertProblem(api.get("/admin/account-types", bearer(token)), 403, "forbidden")
            assertProblem(api.put("/admin/account-types/GOLD", """{"overdraftLimit":1}""", bearer(token)), 403, "forbidden")
        }
        assertThat(api.put("/accounts/$accountId/type", """{"type":"GOLD"}""").status).isEqualTo(200)
        assertThat(api.get("/admin/account-types").status).isEqualTo(200)
    }

    // --- actuator ---

    @Test
    fun `health is open without details, and its details and the metrics are only for admins`() {
        val anonymousHealth = anonymous.get("/actuator/health")
        val customerMetrics = api.get("/actuator/metrics", asCustomer(owner))
        val adminHealth = api.get("/actuator/health")

        assertThat(anonymousHealth.status).isEqualTo(200)
        assertThat(anonymousHealth.body!!.get("status").asString()).isEqualTo("UP")
        assertThat(anonymousHealth.body.has("components")).isFalse()
        assertProblem(anonymous.get("/actuator/metrics"), 401, "unauthorized")
        assertProblem(customerMetrics, 403, "forbidden")
        assertThat(api.get("/actuator/metrics").status).isEqualTo(200)
        assertThat(adminHealth.body!!.has("components")).isTrue()
    }

    // --- deny by default ---

    @Test
    fun `every route of the service has a rule, an admin is never 403 on a mapped route`() {
        val accountId = openAccountOf(owner)
        val routes =
            handlerMapping.handlerMethods.keys
                .flatMap { info -> routesOf(info) }
                .filterNot { (_, pattern) -> pattern.startsWith("/v3") || pattern.startsWith("/swagger") || pattern == "/error" }

        assertThat(routes).isNotEmpty()
        for ((method, pattern) in routes) {
            val path = pattern.replace("{id}", accountId).replace("{type}", "GOLD")
            val response = api.send(method, path, body = "{}", headers = key())

            assertThat(response.status).describedAs("$method $path: ${response.rawBody}").isNotIn(401, 403)
        }
    }

    // --- helpers ---

    private fun routesOf(info: RequestMappingInfo): List<Pair<HttpMethod, String>> =
        info.patternValues.flatMap { pattern -> info.methodsCondition.methods.map { HttpMethod.valueOf(it.name) to pattern } }

    private fun key(value: String = UUID.randomUUID().toString()) = mapOf("Idempotency-Key" to value)

    private fun openAccountOf(customerId: UUID): String {
        val response = api.openAccount(customerId)
        assertThat(response.status).describedAs(response.rawBody).isEqualTo(201)
        return response.body!!.get("id").asString()
    }

    private fun entryCount(accountId: String): Long =
        ownerJdbc
            .sql("select count(*) from ledger_entry where account_id = :id")
            .param("id", UUID.fromString(accountId))
            .query(Long::class.java)
            .single()
}
