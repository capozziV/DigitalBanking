package io.github.capozziv.digitalbanking.transfers.bootstrap.saga

import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitGateway
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs.Companion.entryCreated
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs.Companion.problem
import io.github.capozziv.digitalbanking.transfers.bootstrap.web.ApiClient
import io.github.capozziv.digitalbanking.transfers.bootstrap.web.ApiResponse
import io.github.capozziv.digitalbanking.transfers.bootstrap.web.assertProblem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import java.time.LocalDate
import java.util.UUID

/**
 * The saga against an account that accounts blocked or closed. A debit refused (422 account-blocked or
 * account-closed) fails the transfer before any value leaves the source; a credit refused because the destination was
 * closed in the middle of the saga compensates it, and the value goes back to the source. In every case the limit
 * reservation is given back.
 */
class BlockedOrClosedAccountSagaIntegrationTest : TransfersIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var dailyLimits: DailyLimitGateway

    private val api by lazy { ApiClient(port) }
    private val stubs by lazy { AccountsStubs(accountsServer) }

    private val source = UUID.randomUUID()
    private val destination = UUID.randomUUID()

    @BeforeEach
    fun resetAccounts() {
        accountsServer.resetAll()
        stubs.accountExists(source)
        stubs.accountExists(destination, customerId = UUID.randomUUID())
    }

    @Test
    fun `a debit refused with 422 account-blocked fails the transfer, credits nothing and gives the limit back`() {
        accountsServer.stubFor(post(urlEqualTo("/accounts/$source/debits")).willReturn(problem(422, "account-blocked")))

        val response = transfer(300_000)

        assertProblem(response, 422, "transfer-failed")
        assertThat(response.body!!.get("reason").asString()).isEqualTo("account-blocked")
        val id = UUID.fromString(response.body.get("transferId").asString())
        assertThat(statusAndReason(id)).isEqualTo("FAILED" to "account-blocked")
        accountsServer.verify(0, postRequestedFor(urlEqualTo("/accounts/$destination/credits")))
        accountsServer.verify(0, postRequestedFor(urlEqualTo("/accounts/$source/credits")))
        assertThat(dailyLimits.usage(source, limitDay(id)).used).isZero()
        assertThat(outboxEventTypes(id)).containsExactly("TransferFailed")
    }

    @Test
    fun `a debit refused with 422 account-closed fails the transfer and gives the limit back`() {
        accountsServer.stubFor(post(urlEqualTo("/accounts/$source/debits")).willReturn(problem(422, "account-closed")))

        val response = transfer(300_000)

        assertProblem(response, 422, "transfer-failed")
        val id = UUID.fromString(response.body!!.get("transferId").asString())
        assertThat(statusAndReason(id)).isEqualTo("FAILED" to "account-closed")
        accountsServer.verify(0, postRequestedFor(urlEqualTo("/accounts/$destination/credits")))
        assertThat(dailyLimits.usage(source, limitDay(id)).used).isZero()
    }

    @Test
    fun `a credit refused with 422 account-closed compensates the transfer, the value goes back and the limit too`() {
        accountsServer.stubFor(post(urlEqualTo("/accounts/$source/debits")).willReturn(entryCreated()))
        accountsServer.stubFor(post(urlEqualTo("/accounts/$destination/credits")).willReturn(problem(422, "account-closed")))
        accountsServer.stubFor(post(urlEqualTo("/accounts/$source/credits")).willReturn(entryCreated()))

        val response = transfer(300_000)

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("status").asString()).isEqualTo("COMPENSATED")
        val id = UUID.fromString(response.body.get("id").asString())
        assertThat(statusAndReason(id).first).isEqualTo("COMPENSATED")
        accountsServer.verify(1, postRequestedFor(urlEqualTo("/accounts/$destination/credits")))
        accountsServer.verify(
            1,
            postRequestedFor(urlEqualTo("/accounts/$source/credits"))
                .withHeader("Idempotency-Key", equalTo("transfer-$id-compensation"))
                .withRequestBody(matchingJsonPath("$.amount", equalTo("300000"))),
        )
        assertThat(dailyLimits.usage(source, limitDay(id)).used).isZero()
        assertThat(outboxEventTypes(id)).containsExactly("TransferCompensated")
    }

    private fun transfer(amount: Long): ApiResponse =
        api.post(
            "/transfers",
            """{"sourceAccountId":"$source","destinationAccountId":"$destination","amount":$amount}""",
            mapOf("Idempotency-Key" to UUID.randomUUID().toString()),
        )

    private fun statusAndReason(id: UUID): Pair<String, String?> =
        ownerJdbc
            .sql("select status, failure_reason from transfer where id = :id")
            .param("id", id)
            .query { rs, _ -> rs.getString("status") to rs.getString("failure_reason") }
            .single()

    /** The day the transfer counted in, so the check never races the midnight of São Paulo. */
    private fun limitDay(id: UUID): LocalDate =
        ownerJdbc
            .sql("select limit_day from transfer where id = :id")
            .param("id", id)
            .query(LocalDate::class.java)
            .single()

    private fun outboxEventTypes(id: UUID): List<String> =
        ownerJdbc
            .sql("select event_type from outbox where aggregate_id = :id order by position")
            .param("id", id.toString())
            .query { rs, _ -> rs.getString("event_type") }
            .list()
}
