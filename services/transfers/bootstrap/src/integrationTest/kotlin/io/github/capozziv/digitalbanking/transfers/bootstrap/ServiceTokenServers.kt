package io.github.capozziv.digitalbanking.transfers.bootstrap

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.extension.requestfilter.RequestFilterAction
import com.github.tomakehurst.wiremock.extension.requestfilter.StubRequestFilterV2
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import io.github.capozziv.digitalbanking.security.ServiceTokenClient
import java.net.URI

/**
 * Test doubles for the machine token: a token endpoint that always issues [SERVICE_TOKEN], and peers (`accounts`,
 * `customer`) that answer 401 to any request without `Authorization: Bearer` [SERVICE_TOKEN], whatever the stubs say.
 * So every test that goes through a peer also proves the service token was sent.
 */
object ServiceTokenServers {
    const val SERVICE_TOKEN = "transfers-test-service-token"
    const val TOKEN_PATH = "/default/token"
    const val CLIENT_ID = "transfers"
    const val CLIENT_SECRET = "test-secret"

    /** A token endpoint (client credentials) on [TOKEN_PATH]. */
    fun tokenServer(): WireMockServer =
        WireMockServer(options().dynamicPort()).apply {
            start()
            stubTokenEndpoint(this)
        }

    /** A started peer that refuses, with 401, every request that does not carry the service token. */
    fun peerRequiringServiceToken(): WireMockServer = WireMockServer(peerOptions()).apply { start() }

    /**
     * Options of a peer that refuses, with 401, every request without the service token; [TOKEN_PATH] stays open, so
     * the same server can also issue the token ([stubTokenEndpoint]).
     */
    fun peerOptions(): WireMockConfiguration = options().dynamicPort().extensions(RequireServiceToken())

    /** A [ServiceTokenClient] that gets its token from [stubTokenEndpoint] on [server]. */
    fun clientOf(server: WireMockServer) =
        ServiceTokenClient(URI.create("http://localhost:${server.port()}$TOKEN_PATH"), CLIENT_ID, CLIENT_SECRET)

    fun stubTokenEndpoint(server: WireMockServer) {
        server.stubFor(
            post(urlEqualTo(TOKEN_PATH)).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{"access_token":"$SERVICE_TOKEN","token_type":"Bearer","expires_in":3600}"""),
            ),
        )
    }

    private class RequireServiceToken : StubRequestFilterV2 {
        override fun getName() = "require-service-token"

        override fun filter(
            request: Request,
            serveEvent: ServeEvent?,
        ): RequestFilterAction =
            if (request.url == TOKEN_PATH || request.getHeader("Authorization") == "Bearer $SERVICE_TOKEN") {
                RequestFilterAction.continueWith(request)
            } else {
                RequestFilterAction.stopWith(
                    responseDefinition()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/problem+json")
                        .withBody("""{"type":"/problems/unauthorized","title":"Unauthorized","status":401}""")
                        .build(),
                )
            }
    }
}
