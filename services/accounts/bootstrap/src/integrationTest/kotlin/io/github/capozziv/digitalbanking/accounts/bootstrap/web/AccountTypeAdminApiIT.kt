package io.github.capozziv.digitalbanking.accounts.bootstrap.web

import io.github.capozziv.digitalbanking.accounts.bootstrap.AccountsIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import java.time.Instant
import java.util.UUID

class AccountTypeAdminApiIT : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }

    /** The limits are shared by every test of the context: restore the seeded values after each test. */
    @AfterEach
    fun restoreSeededLimits() {
        SEEDED_LIMITS.forEach { (type, limit) ->
            ownerJdbc
                .sql("update account_type set overdraft_limit = :limit where type = :type")
                .param("limit", limit)
                .param("type", type)
                .update()
        }
    }

    @Test
    fun `list returns every type in declaration order with the seeded limits and updatedAt`() {
        val response = api.get("/admin/account-types")

        assertThat(response.status).isEqualTo(200)
        val items = response.body!!.toList()
        assertThat(items.map { it.get("type").asString() to it.get("overdraftLimit").asLong() })
            .containsExactlyElementsOf(SEEDED_LIMITS.toList())
        items.forEach {
            assertThat(it.propertyNames()).containsExactlyInAnyOrder("type", "overdraftLimit", "updatedAt")
            assertThat(Instant.parse(it.get("updatedAt").asString())).isNotNull()
        }
    }

    @Test
    fun `changing a limit returns 200 with the stored row and applies to accounts of the type`() {
        val before = Instant.now()

        val response = api.put("/admin/account-types/GOLD", """{"overdraftLimit":300000}""")

        assertThat(response.status).isEqualTo(200)
        val body = response.body!!
        assertThat(body.get("type").asString()).isEqualTo("GOLD")
        assertThat(body.get("overdraftLimit").asLong()).isEqualTo(300_000)
        assertThat(Instant.parse(body.get("updatedAt").asString())).isAfterOrEqualTo(before.minusSeconds(1))
        val listed = api.get("/admin/account-types").body!!.first { it.get("type").asString() == "GOLD" }
        assertThat(listed).isEqualTo(body)

        val accountId = openGoldAccount()
        assertThat(
            api
                .get("/accounts/$accountId")
                .body!!
                .get("overdraftLimit")
                .asLong(),
        ).isEqualTo(300_000)
    }

    @Test
    fun `a limit of zero is accepted`() {
        val response = api.put("/admin/account-types/DIAMOND", """{"overdraftLimit":0}""")

        assertThat(response.status).isEqualTo(200)
        assertThat(response.body!!.get("overdraftLimit").asLong()).isEqualTo(0)
    }

    @Test
    fun `an unknown or lowercase type in the path is 404 account-type-not-found`() {
        assertProblem(api.put("/admin/account-types/PLATINUM", """{"overdraftLimit":100}"""), 404, "account-type-not-found")
        assertProblem(api.put("/admin/account-types/gold", """{"overdraftLimit":100}"""), 404, "account-type-not-found")
    }

    @Test
    fun `a limit that is missing, negative, fractional or text is 400 validation-error`() {
        for (body in listOf("""{}""", """{"overdraftLimit":-1}""", """{"overdraftLimit":1.5}""", """{"overdraftLimit":"x"}""")) {
            val response = api.put("/admin/account-types/GOLD", body)

            assertProblem(response, 400, "validation-error")
            assertThat(response.errorFields()).describedAs(body).containsExactly("overdraftLimit")
        }
        assertThat(
            api
                .get("/admin/account-types")
                .body!!
                .first { it.get("type").asString() == "GOLD" }
                .get("overdraftLimit")
                .asLong(),
        ).isEqualTo(200_000)
    }

    private fun openGoldAccount(): String {
        val opened =
            api.post(
                "/accounts",
                """{"customerId":"${UUID.randomUUID()}"}""",
                mapOf("Idempotency-Key" to UUID.randomUUID().toString()),
            )
        val accountId = opened.body!!.get("id").asString()
        assertThat(api.put("/accounts/$accountId/type", """{"type":"GOLD"}""").status).isEqualTo(200)
        return accountId
    }

    private companion object {
        val SEEDED_LIMITS =
            linkedMapOf(
                "FREE" to 0L,
                "STANDARD" to 50_000L,
                "GOLD" to 200_000L,
                "BLACK" to 1_000_000L,
                "DIAMOND" to 5_000_000L,
            )
    }
}
