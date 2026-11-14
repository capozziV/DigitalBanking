package io.github.capozziv.digitalbanking.e2e

import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Gets access tokens from the `mock-oauth2-server` of the environment, the way `infra/local/auth/token.sh` does: a user
 * token (password grant, `sub` = the customer id, the `admin` group on request) for a customer or an admin, and a
 * machine token (client credentials, scope `digitalbanking/internal`). The issuer config of the local environment
 * (`infra/local/auth/mock-oauth2-config.json`) maps the request to the claims. Tokens live one hour, longer than any
 * test class, so each one is fetched once and cached.
 */
class TokenIssuer(
    private val issuer: String,
) {
    private val cache = ConcurrentHashMap<String, String>()

    /** Token of the customer [customerId]: a user without the `admin` group. */
    fun customer(customerId: UUID): String =
        cache.computeIfAbsent("customer:$customerId") { request(USER_CLIENT, "grant_type=password&username=$customerId&password=unused") }

    /** Token of the admin [userId]: a user in the `admin` group. */
    fun admin(userId: UUID = ADMIN_ID): String =
        cache.computeIfAbsent("admin:$userId") {
            request(USER_CLIENT, "grant_type=password&username=$userId&password=unused&user_groups=admin")
        }

    /** Machine token of [clientId], as `transfers` and `accounts` get for their calls. */
    fun service(clientId: String = "e2e"): String =
        cache.computeIfAbsent("service:$clientId") {
            request(clientId, "grant_type=client_credentials&scope=${URLEncoder.encode(INTERNAL_SCOPE, Charsets.UTF_8)}")
        }

    private fun request(
        clientId: String,
        form: String,
    ): String {
        val basic = Base64.getEncoder().encodeToString("$clientId:$CLIENT_SECRET".toByteArray())
        val request =
            HttpRequest
                .newBuilder(URI.create("$issuer/token"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Basic $basic")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == 200) { "token from $issuer: ${response.statusCode()} ${response.body()}" }
        return mapper.readTree(response.body()).get("access_token").asString()
    }

    companion object {
        /** The admin of the tests; any UUID works, the group makes the admin. */
        val ADMIN_ID: UUID = UUID.fromString("00000000-0000-7000-8000-00000000ad01")

        private const val USER_CLIENT = "web"

        /** The mock accepts any secret; a test-only value. */
        private const val CLIENT_SECRET = "local-secret"
        private const val INTERNAL_SCOPE = "digitalbanking/internal"
        private val mapper: JsonMapper = JsonMapper.builder().build()
        private val http: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
    }
}
