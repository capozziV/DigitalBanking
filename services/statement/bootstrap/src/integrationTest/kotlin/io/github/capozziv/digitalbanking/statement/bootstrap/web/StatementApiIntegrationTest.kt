package io.github.capozziv.digitalbanking.statement.bootstrap.web

import io.github.capozziv.digitalbanking.statement.application.port.StatementRepository
import io.github.capozziv.digitalbanking.statement.domain.EntryKind
import io.github.capozziv.digitalbanking.statement.domain.StatementEntry
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import tools.jackson.databind.JsonNode
import java.time.Instant
import java.util.UUID

/** The statement API: cursor pagination, the entry fields, Problem Details and the correlation id. */
class StatementApiIntegrationTest : StatementWebIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var repository: StatementRepository

    private val api by lazy { ApiClient(port) }

    private val account = UUID.randomUUID()
    private val owner = UUID.randomUUID()

    /** The owner of [account] by default; the owner check itself is in [StatementOwnershipIntegrationTest]. */
    private fun get(
        path: String,
        headers: Map<String, String> = emptyMap(),
        bearer: String = JwtTestSupport.customerToken(owner),
    ) = api.get(path, headers, bearer)

    @BeforeEach
    fun registerAccount() {
        repository.insertAccount(account, owner)
    }

    @Test
    fun `three entries come newest first with the fields of the spec`() {
        val debit = entry(3, EntryKind.DEBIT, amount = 2_500, balanceAfter = -1_500, description = null)
        val credit = entry(2, EntryKind.CREDIT, amount = 4_000, balanceAfter = 1_000, description = "salary")
        val first = entry(1, EntryKind.CREDIT, amount = 1_000, balanceAfter = 1_000, description = "deposit")
        listOf(first, credit, debit).forEach(repository::insertEntry)

        val response = get("/accounts/$account/statement")

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        val body = response.body!!
        assertThat(body.get("accountId").asString()).isEqualTo(account.toString())
        assertThat(body.get("nextCursor").isNull).isTrue()
        val entries = body.get("entries")
        assertThat((0 until entries.size()).map { entries.get(it).get("sequence").asLong() }).containsExactly(3L, 2L, 1L)
        val top = entries.get(0)
        assertThat(top.get("entryId").asString()).isEqualTo(debit.entryId.toString())
        assertThat(top.get("kind").asString()).isEqualTo("DEBIT")
        assertThat(top.get("amount").asLong()).isEqualTo(2_500)
        assertThat(top.get("balanceAfter").asLong()).isEqualTo(-1_500)
        assertThat(top.get("description").isNull).isTrue()
        assertThat(Instant.parse(top.get("createdAt").asString())).isEqualTo(debit.createdAt)
        assertThat(entries.get(1).get("description").asString()).isEqualTo("salary")
        assertThat(entries.get(1).get("kind").asString()).isEqualTo("CREDIT")
    }

    @Test
    fun `the cursor walks every page until it is null`() {
        (1L..5L).forEach { repository.insertEntry(entry(it)) }

        val seen = mutableListOf<Long>()
        var path = "/accounts/$account/statement?limit=2"
        var pages = 0
        while (true) {
            val response = get(path)
            assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
            seen += sequences(response.body!!)
            pages++
            val next = response.body.get("nextCursor")
            if (next.isNull) break
            path = "/accounts/$account/statement?limit=2&cursor=${next.asString()}"
        }

        assertThat(seen).containsExactly(5L, 4L, 3L, 2L, 1L)
        assertThat(pages).isEqualTo(3)
    }

    @Test
    fun `the default page has 20 entries`() {
        (1L..25L).forEach { repository.insertEntry(entry(it)) }

        val response = get("/accounts/$account/statement")

        assertThat(response.body!!.get("entries").size()).isEqualTo(20)
        assertThat(response.body.get("nextCursor").isNull).isFalse()
    }

    @Test
    fun `a known account without entries gets an empty page`() {
        val response = get("/accounts/$account/statement")

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("accountId").asString()).isEqualTo(account.toString())
        assertThat(response.body.get("entries").size()).isZero()
        assertThat(response.body.get("nextCursor").isNull).isTrue()
    }

    @Test
    fun `an unknown account is 404 account-not-found`() {
        assertProblem(get("/accounts/${UUID.randomUUID()}/statement"), 404, "account-not-found")
    }

    @ParameterizedTest
    @ValueSource(strings = ["0", "101", "-1", "abc"])
    fun `a limit outside 1 to 100 or not a number is 400 validation-error`(limit: String) {
        val response = get("/accounts/$account/statement?limit=$limit")

        assertProblem(response, 400, "validation-error")
        assertThat(response.errorFields()).containsExactly("limit")
    }

    @Test
    fun `the limits 1 and 100 are accepted`() {
        repository.insertEntry(entry(1))

        assertThat(get("/accounts/$account/statement?limit=1").status).isEqualTo(200)
        assertThat(get("/accounts/$account/statement?limit=100").status).isEqualTo(200)
    }

    @ParameterizedTest
    @ValueSource(strings = ["%25%25%25", "abc", "djE6MA", "djE6eA"])
    fun `a cursor that was not issued by the API is 400 validation-error`(cursor: String) {
        val response = get("/accounts/$account/statement?cursor=$cursor")

        assertProblem(response, 400, "validation-error")
        assertThat(response.errorFields()).containsExactly("cursor")
    }

    @Test
    fun `the cursor past the oldest entry gets an empty page without a next cursor`() {
        repository.insertEntry(entry(1))

        val response = get("/accounts/$account/statement?cursor=djE6MQ")

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("entries").size()).isZero()
        assertThat(response.body.get("nextCursor").isNull).isTrue()
    }

    @Test
    fun `a valid cursor on an unknown account gets an empty page for an admin, not 404`() {
        val unknown = UUID.randomUUID()

        val response = get("/accounts/$unknown/statement?cursor=djE6NQ", bearer = JwtTestSupport.adminToken(UUID.randomUUID()))

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("accountId").asString()).isEqualTo(unknown.toString())
        assertThat(response.body.get("entries").size()).isZero()
        assertThat(response.body.get("nextCursor").isNull).isTrue()
    }

    @Test
    fun `an accountId that is not a UUID is 400 validation-error`() {
        val response = get("/accounts/nope/statement")

        assertProblem(response, 400, "validation-error")
        assertThat(response.errorFields()).containsExactly("accountId")
    }

    @Test
    fun `the correlation id is returned, reused when sent and generated when not`() {
        val sent = get("/accounts/${UUID.randomUUID()}/statement", mapOf("X-Correlation-Id" to "corr-123"))
        val generated = get("/accounts/${UUID.randomUUID()}/statement")

        assertThat(sent.header("X-Correlation-Id")).isEqualTo("corr-123")
        assertThat(generated.header("X-Correlation-Id")).isNotBlank()
    }

    private fun sequences(body: JsonNode): List<Long> {
        val entries = body.get("entries")
        return (0 until entries.size()).map { entries.get(it).get("sequence").asLong() }
    }

    private fun entry(
        sequence: Long,
        kind: EntryKind = EntryKind.CREDIT,
        amount: Long = 1_000,
        balanceAfter: Long = 1_000,
        description: String? = "entry $sequence",
    ) = StatementEntry(
        entryId = UUID.randomUUID(),
        accountId = account,
        sequence = sequence,
        kind = kind,
        amountCents = amount,
        balanceAfterCents = balanceAfter,
        description = description,
        createdAt = Instant.parse("2026-11-10T12:00:00Z").plusSeconds(sequence),
    )
}
