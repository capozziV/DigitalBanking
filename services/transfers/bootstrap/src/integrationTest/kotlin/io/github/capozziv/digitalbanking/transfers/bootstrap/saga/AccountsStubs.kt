package io.github.capozziv.digitalbanking.transfers.bootstrap.saga

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.stubbing.Scenario
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import java.util.UUID

/** WireMock answers of `accounts` shared by the saga integration tests. */
class AccountsStubs(
    private val server: WireMockServer,
) {
    fun accountExists(id: UUID) {
        server.stubFor(
            get(urlEqualTo("/accounts/$id")).willReturn(
                json(
                    200,
                    """{"id":"$id","customerId":"${UUID.randomUUID()}","type":"FREE","balance":0,"overdraftLimit":0,"availableBalance":0}""",
                ),
            ),
        )
    }

    /**
     * Answers the requests matched by [request] with [responses] in order; the last one repeats from then on.
     * [request] builds a new mapping per response (a builder reused for two stubs keeps one stub ID).
     */
    fun sequence(
        request: () -> MappingBuilder,
        vararg responses: ResponseDefinitionBuilder,
    ) {
        val scenario = "scenario-${UUID.randomUUID()}"
        responses.forEachIndexed { i, response ->
            val state = if (i == 0) Scenario.STARTED else "step-$i"
            val mapping = request().inScenario(scenario).whenScenarioStateIs(state).willReturn(response)
            if (i < responses.size - 1) mapping.willSetStateTo("step-${i + 1}")
            server.stubFor(mapping)
        }
    }

    /** Requests received for the paths of [accountId] (debits, credits and entry lookups). */
    fun requestsFor(accountId: UUID): List<LoggedRequest> =
        server.allServeEvents
            .map { it.request }
            .filter { it.url.startsWith("/accounts/$accountId/") }
            .sortedBy { it.loggedDate }

    companion object {
        fun entryCreated(): ResponseDefinitionBuilder =
            json(
                201,
                """{"id":"${UUID.randomUUID()}","accountId":"${UUID.randomUUID()}","sequence":1,"kind":"DEBIT","amount":1,""" +
                    """"balanceAfter":0,"createdAt":"2026-10-27T10:00:00Z"}""",
            )

        fun problem(
            status: Int,
            slug: String,
        ): ResponseDefinitionBuilder =
            aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/problem+json")
                .withBody("""{"type":"/problems/$slug","title":"t","status":$status}""")

        fun entriesPath(accountId: UUID) = urlPathEqualTo("/accounts/$accountId/entries")

        private fun json(
            status: Int,
            body: String,
        ): ResponseDefinitionBuilder = aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody(body)
    }
}
