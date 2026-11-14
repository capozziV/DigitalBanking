package io.github.capozziv.digitalbanking.statement.bootstrap.web

import io.github.capozziv.digitalbanking.statement.application.port.StatementRepository
import io.github.capozziv.digitalbanking.statement.domain.EntryKind
import io.github.capozziv.digitalbanking.statement.domain.StatementEntry
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** JWT on the statement: 401 without a valid token, the owner or an admin reads, anyone else gets the 404 of an unknown account. */
class StatementOwnershipIntegrationTest : StatementWebIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var repository: StatementRepository

    private val api by lazy { ApiClient(port) }

    private val account = UUID.randomUUID()
    private val owner = UUID.randomUUID()
    private val path get() = "/accounts/$account/statement"

    private fun givenAccountWithOneEntry() {
        repository.insertAccount(account, owner)
        repository.insertEntry(
            StatementEntry(UUID.randomUUID(), account, 1, EntryKind.CREDIT, 1_000, 1_000, "deposit", Instant.parse("2026-11-13T12:00:00Z")),
        )
    }

    @Test
    fun `without a token it is 401 unauthorized`() {
        givenAccountWithOneEntry()

        val response = api.get(path)

        assertProblem(response, 401, "unauthorized")
        assertThat(response.header("X-Correlation-Id")).isNotBlank()
    }

    @Test
    fun `an expired token, one from another issuer and a malformed one are 401`() {
        givenAccountWithOneEntry()
        val expired = JwtTestSupport.token(owner.toString(), expiresIn = Duration.ofMinutes(-5))
        val otherIssuer = JwtTestSupport.token(owner.toString(), issuer = "http://other.test")

        listOf(expired, otherIssuer, "not-a-jwt").forEach { token ->
            assertProblem(api.get(path, bearer = token), 401, "unauthorized")
        }
    }

    @Test
    fun `the owner reads the statement`() {
        givenAccountWithOneEntry()

        val response = api.get(path, bearer = JwtTestSupport.customerToken(owner))

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("entries").size()).isEqualTo(1)
    }

    @Test
    fun `an account of another customer is 404 account-not-found, like an unknown account`() {
        givenAccountWithOneEntry()

        val foreign = api.get(path, bearer = JwtTestSupport.customerToken(UUID.randomUUID()))
        val unknown = api.get("/accounts/${UUID.randomUUID()}/statement", bearer = JwtTestSupport.customerToken(UUID.randomUUID()))

        assertProblem(foreign, 404, "account-not-found")
        assertProblem(unknown, 404, "account-not-found")
        assertThat(foreign.body!!.get("title")).isEqualTo(unknown.body!!.get("title"))
        assertThat(foreign.rawBody).doesNotContain("deposit")
    }

    @Test
    fun `an admin reads the statement of any customer`() {
        givenAccountWithOneEntry()

        val response = api.get(path, bearer = JwtTestSupport.adminToken(UUID.randomUUID()))

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("entries").size()).isEqualTo(1)
    }

    @Test
    fun `an admin who is also the customer of the account reads it`() {
        givenAccountWithOneEntry()

        assertThat(api.get(path, bearer = JwtTestSupport.adminToken(owner)).status).isEqualTo(200)
    }

    @Test
    fun `a service token is not a statement reader`() {
        givenAccountWithOneEntry()

        assertThat(api.get(path, bearer = JwtTestSupport.serviceToken()).status).isEqualTo(403)
    }

    @Test
    fun `the actuator is open for health only, metrics are for an admin`() {
        assertThat(api.get("/actuator/health").status).isEqualTo(200)
        assertProblem(api.get("/actuator/metrics"), 401, "unauthorized")
        assertProblem(api.get("/actuator/metrics", bearer = JwtTestSupport.customerToken(owner)), 403, "forbidden")
        assertThat(api.get("/actuator/metrics", bearer = JwtTestSupport.adminToken(owner)).status).isEqualTo(200)
    }

    @Test
    fun `a route nobody declared is denied`() {
        assertProblem(api.get("/accounts", bearer = JwtTestSupport.adminToken(owner)), 403, "forbidden")
    }
}
