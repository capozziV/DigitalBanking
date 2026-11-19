package io.github.capozziv.digitalbanking.accounts.bootstrap.web

import io.github.capozziv.digitalbanking.accounts.bootstrap.AccountsIntegrationTest
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import java.util.UUID

/** `GET /accounts/me` and `GET /accounts/recipient`: how the BFF finds an account without knowing its id. */
class AccountOwnLookupApiIntegrationTest : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }
    private val anonymous by lazy { ApiClient(port, defaultToken = null) }

    @Test
    fun `me returns the caller account`() {
        val customerId = UUID.randomUUID()
        val accountId = openAccountOf(customerId)

        val response = api.get("/accounts/me", asCustomer(customerId))

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("id").asString()).isEqualTo(accountId)
        assertThat(response.body.get("customerId").asString()).isEqualTo(customerId.toString())
        assertThat(response.body.get("balance").asLong()).isEqualTo(0L)
    }

    @Test
    fun `me returns 404 when the caller has no account`() {
        assertProblem(api.get("/accounts/me", asCustomer(UUID.randomUUID())), 404, "account-not-found")
    }

    @Test
    fun `an admin reads its own account through me, or gets 404 without one`() {
        val adminWithAccount = UUID.randomUUID()
        val accountId = openAccountOf(adminWithAccount)

        val response = api.get("/accounts/me", bearer(JwtTestSupport.adminToken(adminWithAccount)))

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("id").asString()).isEqualTo(accountId)
        assertThat(response.body.get("customerId").asString()).isEqualTo(adminWithAccount.toString())

        assertProblem(api.get("/accounts/me", bearer(JwtTestSupport.adminToken(UUID.randomUUID()))), 404, "account-not-found")
    }

    @Test
    fun `me ignores a closed account`() {
        val customerId = UUID.randomUUID()
        val closedId = openAccountOf(customerId)
        close(closedId, customerId)

        assertProblem(api.get("/accounts/me", asCustomer(customerId)), 404, "account-not-found")

        val reopenedId = openAccountOf(customerId)
        val response = api.get("/accounts/me", asCustomer(customerId))
        assertThat(response.body!!.get("id").asString()).isEqualTo(reopenedId).isNotEqualTo(closedId)
    }

    @Test
    fun `recipient returns only the account id`() {
        val customerId = UUID.randomUUID()
        val accountId = openAccountOf(customerId)

        val response = api.get("/accounts/recipient?customerId=$customerId", asCustomer(UUID.randomUUID()))

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.propertyNames()).containsExactly("accountId")
        assertThat(response.body.get("accountId").asString()).isEqualTo(accountId)
    }

    @Test
    fun `recipient returns the account id of a blocked account`() {
        val customerId = UUID.randomUUID()
        val accountId = openAccountOf(customerId)
        val blocked = api.put("/admin/accounts/$accountId/status", """{"status":"BLOCKED"}""")
        assertThat(blocked.status).describedAs(blocked.rawBody).isEqualTo(200)

        val response = api.get("/accounts/recipient?customerId=$customerId", asCustomer(UUID.randomUUID()))

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("accountId").asString()).isEqualTo(accountId)
    }

    @Test
    fun `recipient returns 404 for a closed account`() {
        val customerId = UUID.randomUUID()
        close(openAccountOf(customerId), customerId)

        assertProblem(api.get("/accounts/recipient?customerId=$customerId", asCustomer(UUID.randomUUID())), 404, "account-not-found")
    }

    @Test
    fun `recipient returns 404 for a customer without account and 400 for a bad customer id`() {
        assertProblem(api.get("/accounts/recipient?customerId=${UUID.randomUUID()}"), 404, "account-not-found")
        assertProblem(api.get("/accounts/recipient?customerId=not-a-uuid"), 400, "validation-error")
        assertProblem(api.get("/accounts/recipient"), 400, "validation-error")
    }

    @Test
    fun `recipient requires authentication`() {
        assertProblem(anonymous.get("/accounts/recipient?customerId=${UUID.randomUUID()}"), 401, "unauthorized")
        assertProblem(anonymous.get("/accounts/me"), 401, "unauthorized")
    }

    @Test
    fun `service role cannot call me or recipient`() {
        val service = bearer(JwtTestSupport.serviceToken())

        assertProblem(api.get("/accounts/me", service), 403, "forbidden")
        assertProblem(api.get("/accounts/recipient?customerId=${UUID.randomUUID()}", service), 403, "forbidden")
    }

    private fun openAccountOf(customerId: UUID): String {
        val response = api.openAccount(customerId)
        assertThat(response.status).describedAs(response.rawBody).isEqualTo(201)
        return response.body!!.get("id").asString()
    }

    private fun close(
        accountId: String,
        customerId: UUID,
    ) {
        val response = api.send(HttpMethod.POST, "/accounts/$accountId/close", headers = asCustomer(customerId))
        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
    }
}
