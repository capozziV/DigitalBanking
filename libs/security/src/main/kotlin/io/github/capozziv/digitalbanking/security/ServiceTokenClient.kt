package io.github.capozziv.digitalbanking.security

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.client.ClientHttpRequestInterceptor
import tools.jackson.core.JacksonException
import tools.jackson.databind.json.JsonMapper
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64

/**
 * Machine token for calls between services: OAuth2 client credentials (`client_secret_basic`) against [tokenUri],
 * asking for [scope].
 *
 * - [accessToken] returns the cached token until [RENEW_BEFORE_EXPIRY] (30 s) before it expires, then fetches a new
 *   one; concurrent callers share a single request to the token endpoint.
 * - Any failure (endpoint down, [timeout], non-2xx, unexpected body) is a [ServiceTokenUnavailableException], an
 *   [IOException], so a `RestClient` with [interceptor] fails with `ResourceAccessException`, the same as an
 *   unreachable peer: the service's gateway already answers that with 503 or a retry. The secret is never logged.
 *
 * Use: `RestClient.builder().requestInterceptor(serviceTokenClient.interceptor())`. The bean is created by
 * [DigitalBankingSecurityAutoConfiguration] when `digitalbanking.security.service-token.token-uri` is set.
 */
class ServiceTokenClient(
    private val tokenUri: URI,
    private val clientId: String,
    private val clientSecret: String,
    private val scope: String = CallerResolver.SERVICE_SCOPE,
    private val timeout: Duration = Duration.ofSeconds(2),
    private val clock: Clock = Clock.systemUTC(),
) {
    private val httpClient =
        HttpClient
            .newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(timeout)
            .build()
    private val mapper = JsonMapper.builder().build()
    private val lock = Any()

    @Volatile
    private var cached: CachedToken? = null

    /** A valid access token, from the cache or from the token endpoint. */
    @Throws(ServiceTokenUnavailableException::class)
    fun accessToken(): String {
        cached?.takeIf { it.isFresh() }?.let { return it.value }
        synchronized(lock) {
            cached?.takeIf { it.isFresh() }?.let { return it.value }
            return fetch().also { cached = it }.value
        }
    }

    /** Puts `Authorization: Bearer <token>` on every request of the `RestClient`. */
    fun interceptor(): ClientHttpRequestInterceptor =
        ClientHttpRequestInterceptor { request, body, execution ->
            request.headers.setBearerAuth(accessToken())
            execution.execute(request, body)
        }

    private fun fetch(): CachedToken {
        val requestedAt = clock.instant()
        val response =
            try {
                httpClient.send(tokenRequest(), HttpResponse.BodyHandlers.ofString())
            } catch (e: IOException) {
                throw unavailable("token endpoint unreachable: ${e.javaClass.simpleName}")
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw unavailable("interrupted while waiting for the token endpoint")
            }
        if (response.statusCode() !in 200..299) throw unavailable("token endpoint answered ${response.statusCode()}")
        val body =
            try {
                mapper.readTree(response.body())
            } catch (_: JacksonException) {
                throw unavailable("token endpoint answered a body that is not JSON")
            }
        val token = body["access_token"]?.takeIf { it.isString }?.asString()
        val expiresIn = body["expires_in"]?.takeIf { it.isNumber }?.asLong()
        if (token.isNullOrBlank() || expiresIn == null) throw unavailable("token endpoint answered without access_token or expires_in")
        return CachedToken(token, requestedAt.plusSeconds(expiresIn))
    }

    private fun tokenRequest(): HttpRequest {
        val credentials = "${formEncode(clientId)}:${formEncode(clientSecret)}"
        val form = "grant_type=client_credentials&scope=${formEncode(scope)}"
        return HttpRequest
            .newBuilder(tokenUri)
            .timeout(timeout)
            .header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(credentials.toByteArray()))
            .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
            .header(HttpHeaders.ACCEPT, "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build()
    }

    private fun unavailable(reason: String): ServiceTokenUnavailableException {
        log.warn("could not get a service token from {}: {}", tokenUri, reason)
        return ServiceTokenUnavailableException(reason)
    }

    private fun formEncode(value: String) = URLEncoder.encode(value, Charsets.UTF_8)

    private inner class CachedToken(
        val value: String,
        private val expiresAt: Instant,
    ) {
        fun isFresh() = clock.instant().isBefore(expiresAt.minus(RENEW_BEFORE_EXPIRY))
    }

    companion object {
        val RENEW_BEFORE_EXPIRY: Duration = Duration.ofSeconds(30)
        private val log = LoggerFactory.getLogger(ServiceTokenClient::class.java)
    }
}

/** No machine token could be obtained; an [IOException] so HTTP clients treat it as a network failure. */
class ServiceTokenUnavailableException(
    message: String,
) : IOException(message)
