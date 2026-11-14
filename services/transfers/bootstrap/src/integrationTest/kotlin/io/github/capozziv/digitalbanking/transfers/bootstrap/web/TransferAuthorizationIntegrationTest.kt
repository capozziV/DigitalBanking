package io.github.capozziv.digitalbanking.transfers.bootstrap.web

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import io.github.capozziv.digitalbanking.transfers.bootstrap.ServiceTokenServers.SERVICE_TOKEN
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs.Companion.entryCreated
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs.Companion.problem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.util.UUID

/**
 * Who may do what in transfers: the token (401/403), the owner of the source account and of the transfer (404 for
 * someone else's), the admin (everything), and the `customer` check before a transfer is created.
 */
class TransferAuthorizationIntegrationTest : TransfersIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    private val api by lazy { ApiClient(port) }
    private val stubs by lazy { AccountsStubs(accountsServer) }

    private val source = UUID.randomUUID()
    private val destination = UUID.randomUUID()
    private val otherCustomer = UUID.randomUUID()

    @BeforeEach
    fun resetAccounts() {
        accountsServer.resetAll()
        stubs.accountExists(source)
        stubs.accountExists(destination, customerId = otherCustomer)
        accountsServer.stubFor(post(urlEqualTo("/accounts/$source/debits")).willReturn(entryCreated()))
        accountsServer.stubFor(post(urlEqualTo("/accounts/$destination/credits")).willReturn(entryCreated()))
    }

    // --- tokens ---

    @Test
    fun `no token is 401 on every route`() {
        val anonymous = api.withToken(null)

        assertProblem(anonymous.post("/transfers", transferJson(), key()), 401, "unauthorized")
        assertProblem(anonymous.get("/transfers/${UUID.randomUUID()}"), 401, "unauthorized")
        assertProblem(anonymous.get("/accounts/$source/daily-limit"), 401, "unauthorized")
        assertThat(accountsServer.allServeEvents).isEmpty()
    }

    @Test
    fun `a service token is 403 on the customer routes`() {
        val service = api.withToken(JwtTestSupport.serviceToken())

        assertProblem(service.post("/transfers", transferJson(), key()), 403, "forbidden")
        assertProblem(service.get("/transfers/${UUID.randomUUID()}"), 403, "forbidden")
        assertProblem(service.get("/accounts/$source/daily-limit"), 403, "forbidden")
    }

    @Test
    fun `every route of the service lets an admin in`() {
        val admin = api.withToken(JwtTestSupport.adminToken(UUID.randomUUID()))
        val routes =
            handlerMapping.handlerMethods.keys.flatMap { info ->
                info.methodsCondition.methods.flatMap { method -> info.patternValues.map { pattern -> method.name to pattern } }
            }

        assertThat(routes).isNotEmpty()
        for ((method, pattern) in routes) {
            val path = pattern.replace(Regex("\\{[^}]+}"), UUID.randomUUID().toString())
            val response = admin.send(HttpMethod.valueOf(method), path)
            assertThat(response.status).describedAs("$method $pattern").isNotIn(401, 403)
        }
    }

    // --- actuator ---

    @Test
    fun `health is open and the rest of the actuator is for admins only`() {
        assertThat(api.withToken(null).get("/actuator/health").status).isEqualTo(200)
        assertProblem(api.get("/actuator/metrics"), 403, "forbidden")
        assertThat(api.withToken(JwtTestSupport.adminToken(UUID.randomUUID())).get("/actuator/metrics").status).isEqualTo(200)
    }

    @Test
    fun `health shows no details to a customer`() {
        val health = api.get("/actuator/health")

        assertThat(health.status).isEqualTo(200)
        assertThat(health.body!!.has("components")).isFalse()
    }

    // --- POST /transfers: the source must be the caller's ---

    @Test
    fun `a source account of another customer is 404 account-not-found, nothing is stored and customer is not asked`() {
        val foreign = UUID.randomUUID()
        stubs.accountExists(foreign, customerId = otherCustomer)
        val before = transferCount()

        val response = api.post("/transfers", transferJson(sourceId = foreign), key())

        assertProblem(response, 404, "account-not-found")
        assertThat(response.body!!.get("accountId").asString()).isEqualTo(foreign.toString())
        assertThat(transferCount()).isEqualTo(before)
        accountsServer.verify(0, postRequestedFor(urlPathMatching("/accounts/.*")))
        assertThat(customerServer.allServeEvents).isEmpty()
    }

    @Test
    fun `an admin transfers from the account of any customer, and that customer is the one checked`() {
        val foreign = UUID.randomUUID()
        stubs.accountExists(foreign, customerId = otherCustomer)
        accountsServer.stubFor(post(urlEqualTo("/accounts/$foreign/debits")).willReturn(entryCreated()))
        stubCustomer(otherCustomer, "ACTIVE")
        val admin = api.withToken(JwtTestSupport.adminToken(UUID.randomUUID()))

        val response = admin.post("/transfers", transferJson(sourceId = foreign), key())

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(201)
        customerServer.verify(1, getRequestedFor(urlEqualTo("/internal/customers/$otherCustomer")))
        assertThat(sourceCustomerOf(UUID.fromString(response.body!!.get("id").asString()))).isEqualTo(otherCustomer)
    }

    @Test
    fun `the transfer records the customer that owns the source`() {
        val response = api.post("/transfers", transferJson(), key())

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(201)
        assertThat(sourceCustomerOf(UUID.fromString(response.body!!.get("id").asString()))).isEqualTo(CUSTOMER_ID)
    }

    // --- POST /transfers: the customer check ---

    @Test
    fun `a blocked customer is 422 customer-blocked and nothing is stored or debited`() {
        stubCustomer(CUSTOMER_ID, "BLOCKED")
        val before = transferCount()

        val response = api.post("/transfers", transferJson(), key())

        assertProblem(response, 422, "customer-blocked")
        assertThat(transferCount()).isEqualTo(before)
        accountsServer.verify(0, postRequestedFor(urlPathMatching("/accounts/.*")))
    }

    @Test
    fun `an unknown customer is 422 customer-not-found and nothing is stored`() {
        customerServer.stubFor(get(urlEqualTo("/internal/customers/$CUSTOMER_ID")).willReturn(problem(404, "customer-not-found")))
        val before = transferCount()

        assertProblem(api.post("/transfers", transferJson(), key()), 422, "customer-not-found")
        assertThat(transferCount()).isEqualTo(before)
    }

    @Test
    fun `customer answering 5xx is 503 customers-unavailable and nothing is stored`() {
        customerServer.stubFor(get(urlEqualTo("/internal/customers/$CUSTOMER_ID")).willReturn(aResponse().withStatus(500)))
        val before = transferCount()

        val response = api.post("/transfers", transferJson(), key())

        assertProblem(response, 503, "customers-unavailable")
        assertThat(response.header("Retry-After")).isEqualTo("1")
        assertThat(transferCount()).isEqualTo(before)
    }

    @Test
    fun `customer slower than the timeout is 503 customers-unavailable and nothing is stored`() {
        customerServer.stubFor(
            get(urlEqualTo("/internal/customers/$CUSTOMER_ID")).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{"id":"$CUSTOMER_ID","status":"ACTIVE"}""")
                    .withFixedDelay(2_000),
            ),
        )
        val before = transferCount()

        assertProblem(api.post("/transfers", transferJson(), key()), 503, "customers-unavailable")
        assertThat(transferCount()).isEqualTo(before)
    }

    // --- calls to the other services carry the machine token ---

    @Test
    fun `accounts and customer receive the service token and the correlation id`() {
        val response = api.post("/transfers", transferJson(), key() + mapOf("X-Correlation-Id" to "corr-token-1"))

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(201)
        val bearer = equalTo("Bearer $SERVICE_TOKEN")
        accountsServer.verify(getRequestedFor(urlEqualTo("/accounts/$source")).withHeader("Authorization", bearer))
        accountsServer.verify(postRequestedFor(urlEqualTo("/accounts/$source/debits")).withHeader("Authorization", bearer))
        accountsServer.verify(postRequestedFor(urlEqualTo("/accounts/$destination/credits")).withHeader("Authorization", bearer))
        customerServer.verify(
            getRequestedFor(urlEqualTo("/internal/customers/$CUSTOMER_ID"))
                .withHeader("Authorization", bearer)
                .withHeader("X-Correlation-Id", equalTo("corr-token-1")),
        )
    }

    // --- GET /transfers/{id} ---

    @Test
    fun `the owner and an admin see a transfer, another customer gets 404 transfer-not-found`() {
        val id =
            api
                .post("/transfers", transferJson(), key())
                .body!!
                .get("id")
                .asString()

        assertThat(api.get("/transfers/$id").status).isEqualTo(200)
        assertThat(api.withToken(JwtTestSupport.adminToken(UUID.randomUUID())).get("/transfers/$id").status).isEqualTo(200)
        assertProblem(api.withToken(JwtTestSupport.customerToken(otherCustomer)).get("/transfers/$id"), 404, "transfer-not-found")
    }

    @Test
    fun `an admin that is the customer sees its own transfer`() {
        val id =
            api
                .post("/transfers", transferJson(), key())
                .body!!
                .get("id")
                .asString()

        assertThat(api.withToken(JwtTestSupport.adminToken(CUSTOMER_ID)).get("/transfers/$id").status).isEqualTo(200)
    }

    @Test
    fun `repeating someone else's request with its key is 404 account-not-found`() {
        val key = key()
        assertThat(api.post("/transfers", transferJson(), key).status).isEqualTo(201)

        val response = api.withToken(JwtTestSupport.customerToken(otherCustomer)).post("/transfers", transferJson(), key)

        assertProblem(response, 404, "account-not-found")
    }

    // --- GET /accounts/{id}/daily-limit ---

    @Test
    fun `the daily limit of the caller's own account is 200`() {
        val response = api.get("/accounts/$source/daily-limit")

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        accountsServer.verify(
            getRequestedFor(urlEqualTo("/accounts/$source")).withHeader("Authorization", equalTo("Bearer $SERVICE_TOKEN")),
        )
    }

    @Test
    fun `the daily limit of another customer's account is 404 account-not-found`() {
        assertProblem(api.get("/accounts/$destination/daily-limit"), 404, "account-not-found")
    }

    @Test
    fun `the daily limit of an account accounts does not know is 404 account-not-found for a customer`() {
        val missing = UUID.randomUUID()
        accountsServer.stubFor(get(urlEqualTo("/accounts/$missing")).willReturn(problem(404, "account-not-found")))

        assertProblem(api.get("/accounts/$missing/daily-limit"), 404, "account-not-found")
    }

    @Test
    fun `the daily limit with accounts unavailable is 503 accounts-unavailable`() {
        accountsServer.stubFor(get(urlEqualTo("/accounts/$source")).willReturn(aResponse().withStatus(503)))

        assertProblem(api.get("/accounts/$source/daily-limit"), 503, "accounts-unavailable")
    }

    @Test
    fun `an admin sees the daily limit of any account`() {
        val response = api.withToken(JwtTestSupport.adminToken(UUID.randomUUID())).get("/accounts/$destination/daily-limit")

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
    }

    // --- helpers ---

    private fun key() = mapOf("Idempotency-Key" to UUID.randomUUID().toString())

    private fun transferJson(sourceId: UUID = source) =
        """{"sourceAccountId":"$sourceId","destinationAccountId":"$destination","amount":10}"""

    private fun transferCount(): Int = ownerJdbc.sql("select count(*) from transfer").query(Int::class.java).single()

    private fun sourceCustomerOf(transferId: UUID): UUID? =
        ownerJdbc
            .sql("select source_customer_id from transfer where id = :id")
            .param("id", transferId)
            .query(UUID::class.java)
            .single()
}
