package io.github.capozziv.digitalbanking.transfers.bootstrap.web

import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs
import io.github.capozziv.digitalbanking.transfers.bootstrap.saga.AccountsStubs.Companion.entryCreated
import io.github.capozziv.digitalbanking.transfers.domain.DailyLimitDay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import java.time.Clock
import java.util.UUID

/** `GET /accounts/{accountId}/daily-limit` against the real Redis and PostgreSQL, with the default limit of 500000. */
class DailyLimitApiIntegrationTest : TransfersIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var clock: Clock

    private val api by lazy { ApiClient(port) }
    private val stubs by lazy { AccountsStubs(accountsServer) }
    private val source = UUID.randomUUID()

    @BeforeEach
    fun resetAccounts() {
        accountsServer.resetAll()
        stubs.accountExists(source)
        accountsServer.stubFor(post(urlEqualTo("/accounts/$source/debits")).willReturn(entryCreated()))
    }

    @Test
    fun `an account that never transferred has the whole limit available`() {
        val untouched = UUID.randomUUID()
        stubs.accountExists(untouched)

        val response = api.get("/accounts/$untouched/daily-limit")

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("limitCents").asLong()).isEqualTo(500_000)
        assertThat(response.body.get("usedCents").asLong()).isZero()
        assertThat(response.body.get("availableCents").asLong()).isEqualTo(500_000)
    }

    @Test
    fun `after a transfer the limit shows what was used and what is left`() {
        val destination = UUID.randomUUID()
        stubs.accountExists(destination)
        accountsServer.stubFor(post(urlEqualTo("/accounts/$destination/credits")).willReturn(entryCreated()))
        val transfer =
            api.post(
                "/transfers",
                """{"sourceAccountId":"$source","destinationAccountId":"$destination","amount":120000}""",
                mapOf("Idempotency-Key" to UUID.randomUUID().toString()),
            )
        assertThat(transfer.status).describedAs(transfer.rawBody).isEqualTo(201)

        val response = api.get("/accounts/$source/daily-limit")

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("day").asString()).isEqualTo(DailyLimitDay.of(clock.instant()).toString())
        assertThat(response.body.get("limitCents").asLong()).isEqualTo(500_000)
        assertThat(response.body.get("usedCents").asLong()).isEqualTo(120_000)
        assertThat(response.body.get("availableCents").asLong()).isEqualTo(380_000)
    }

    @Test
    fun `an id that is not a UUID is a validation error`() {
        assertProblem(api.get("/accounts/not-a-uuid/daily-limit"), 400, "validation-error")
    }
}
