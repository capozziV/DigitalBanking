package io.github.capozziv.digitalbanking.accounts.bootstrap

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import java.time.Duration
import java.util.UUID

/**
 * WireMock playing the `transfers` service (`GET /internal/accounts/{id}/pending-transfers`), so the accounts service
 * runs its real HTTP gateway. Without `Authorization: Bearer ...` the answer is 401; with it, no account has a pending
 * transfer unless [answer] sets another [Behavior] for it. One server for every integration test of the JVM.
 */
class TransfersServiceStub private constructor(
    private val server: WireMockServer,
) {
    sealed interface Behavior {
        data class Pending(
            val count: Int,
        ) : Behavior

        data object ServerError : Behavior

        /** Answers `{"count":0}`, but only after [delay]. */
        data class Slow(
            val delay: Duration,
        ) : Behavior
    }

    val baseUrl: String get() = "http://127.0.0.1:${server.port()}"

    fun answer(
        accountId: UUID,
        behavior: Behavior,
    ) {
        val response =
            when (behavior) {
                is Behavior.Pending -> okJson("""{"count":${behavior.count}}""")
                Behavior.ServerError -> aResponse().withStatus(500).withBody("""{"status":500}""")
                is Behavior.Slow -> okJson("""{"count":0}""").withFixedDelay(behavior.delay.toMillis().toInt())
            }
        server.stubFor(
            get(urlEqualTo(pathOf(accountId)))
                .atPriority(1)
                .withHeader("Authorization", matching("Bearer .+"))
                .willReturn(response),
        )
    }

    /** Every lookup received for [accountId], with its headers. */
    fun lookupsOf(accountId: UUID): List<LoggedRequest> = server.findAll(getRequestedFor(urlEqualTo(pathOf(accountId))))

    private fun pathOf(accountId: UUID) = "/internal/accounts/$accountId/pending-transfers"

    companion object {
        val instance: TransfersServiceStub by lazy { start() }

        private fun start(): TransfersServiceStub {
            val server = WireMockServer(options().dynamicPort().bindAddress("127.0.0.1"))
            server.start()
            server.stubFor(
                get(urlPathMatching("/internal/accounts/[^/]+/pending-transfers"))
                    .atPriority(10)
                    .withHeader("Authorization", matching("Bearer .+"))
                    .willReturn(okJson("""{"count":0}""")),
            )
            server.stubFor(get(anyUrl()).atPriority(100).willReturn(aResponse().withStatus(401).withBody("""{"status":401}""")))
            return TransfersServiceStub(server)
        }
    }
}
