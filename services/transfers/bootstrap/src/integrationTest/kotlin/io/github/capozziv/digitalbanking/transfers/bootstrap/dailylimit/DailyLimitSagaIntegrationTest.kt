package io.github.capozziv.digitalbanking.transfers.bootstrap.dailylimit

import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitGateway
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs.Companion.entryCreated
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs.Companion.problem
import io.github.capozziv.digitalbanking.transfers.bootstrap.web.ApiClient
import io.github.capozziv.digitalbanking.transfers.bootstrap.web.ApiResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import java.time.LocalDate
import java.util.UUID

/** The daily limit inside the saga, through `POST /transfers`, with the default limit of 500000 cents. */
class DailyLimitSagaIntegrationTest : TransfersIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var dailyLimits: DailyLimitGateway

    private val api by lazy { ApiClient(port) }
    private val stubs by lazy { AccountsStubs(accountsServer) }

    private val source = UUID.randomUUID()

    @BeforeEach
    fun resetAccounts() {
        accountsServer.resetAll()
        stubs.accountExists(source)
    }

    @Test
    fun `a second transfer that passes the daily limit fails with daily-limit-exceeded without calling accounts`() {
        accountsServer.stubFor(post(urlEqualTo("/accounts/$source/debits")).willReturn(entryCreated()))
        val firstDestination = destinationThatCredits()
        val secondDestination = destinationThatCredits()

        val first = transfer(firstDestination, 300_000)
        val second = transfer(secondDestination, 300_000)

        assertThat(first.status).describedAs(first.rawBody).isEqualTo(201)
        assertThat(second.status).describedAs(second.rawBody).isEqualTo(422)
        val secondId = UUID.fromString(second.body!!.get("transferId").asString())
        assertThat(statusAndReason(secondId)).isEqualTo("FAILED" to "daily-limit-exceeded")
        assertThat(limitReserved(secondId)).isFalse()
        accountsServer.verify(1, postRequestedFor(urlEqualTo("/accounts/$source/debits")))
    }

    @Test
    fun `a debit refused for lack of balance after the reservation gives the limit back`() {
        accountsServer.stubFor(
            post(urlEqualTo("/accounts/$source/debits")).willReturn(problem(422, "insufficient-available-balance")),
        )

        val refused = transfer(destinationThatCredits(), 300_000)

        assertThat(refused.status).describedAs(refused.rawBody).isEqualTo(422)
        val refusedId = UUID.fromString(refused.body!!.get("transferId").asString())
        assertThat(statusAndReason(refusedId)).isEqualTo("FAILED" to "insufficient-available-balance")
        // Reserved first, so the reservation was made and then released.
        assertThat(limitReserved(refusedId)).isTrue()
        assertThat(dailyLimits.usage(source, limitDay(refusedId)).used).isZero()

        accountsServer.resetAll()
        stubs.accountExists(source)
        accountsServer.stubFor(post(urlEqualTo("/accounts/$source/debits")).willReturn(entryCreated()))
        val full = transfer(destinationThatCredits(), 500_000)

        assertThat(full.status).describedAs(full.rawBody).isEqualTo(201)
    }

    private fun destinationThatCredits(): UUID {
        val destination = UUID.randomUUID()
        stubs.accountExists(destination)
        accountsServer.stubFor(post(urlEqualTo("/accounts/$destination/credits")).willReturn(entryCreated()))
        return destination
    }

    private fun transfer(
        destination: UUID,
        amount: Long,
    ): ApiResponse =
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

    private fun limitReserved(id: UUID): Boolean =
        ownerJdbc
            .sql("select limit_reserved from transfer where id = :id")
            .param("id", id)
            .query(Boolean::class.java)
            .single()
}
