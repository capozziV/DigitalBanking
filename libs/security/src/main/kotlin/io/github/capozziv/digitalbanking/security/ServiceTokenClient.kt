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
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Machine token for calls between services: OAuth2 client credentials (`client_secret_basic`) against [tokenUri],
 * asking for [scope].
 *
 * - [accessToken] returns the cached token until [RENEW_BEFORE_EXPIRY] (30 s) before it expires, or until half of its
 *   validity when the token lives 60 s or less; then it fetches a new one. Concurrent callers share a single request
 *   to the token endpoint.
 * - Any failure (endpoint down, timeout, non-2xx, unexpected body) is a [ServiceTokenUnavailableException], an
 *   [IOException], so a `RestClient` with [interceptor] fails with `ResourceAccessException`, the same as an
 *   unreachable peer: the service's gateway already answers that with 503 or a retry. The secret is never logged.
 * - A failure is shared: for [FAILURE_BACKOFF] (1 s) after it, every call fails at once without asking the endpoint
 *   again, including the calls that were waiting for the failed request.
 * - Worst case of one call: [maxFetchDuration] = [connectTimeout] + [readTimeout] (500 ms + 1.5 s = 2 s by default).
 *   It bounds the whole token request, body included, and a call never waits for more than one token request. A
 *   caller with a time budget per call (the `call-budget` of `transfers`) must add it to its own HTTP timeouts.
 *
 * Use: `RestClient.builder().requestInterceptor(serviceTokenClient.interceptor())`. The bean is created by
 * [ServiceTokenAutoConfiguration] when `digitalbanking.security.service-token.token-uri` is set.
 */
class ServiceTokenClient(
    private val tokenUri: URI,
    private val clientId: String,
    private val clientSecret: String,
    private val scope: String = CallerResolver.SERVICE_SCOPE,
    private val connectTimeout: Duration = DEFAULT_CONNECT_TIMEOUT,
    private val readTimeout: Duration = DEFAULT_READ_TIMEOUT,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val httpClient =
        HttpClient
            .newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(connectTimeout)
            .build()
    private val mapper = JsonMapper.builder().build()
    private val lock = Any()

    /** The longest one call to [accessToken] can take: [connectTimeout] + [readTimeout]. */
    val maxFetchDuration: Duration = connectTimeout.plus(readTimeout)

    @Volatile
    private var cached: CachedToken? = null

    @Volatile
    private var lastFailure: RecentFailure? = null

    /** A valid access token, from the cache or from the token endpoint. */
    @Throws(ServiceTokenUnavailableException::class)
    fun accessToken(): String {
        cached?.takeIf { it.isFresh() }?.let { return it.value }
        lastFailure?.takeIf { it.isRecent() }?.let { throw it.shared() }
        synchronized(lock) {
            cached?.takeIf { it.isFresh() }?.let { return it.value }
            lastFailure?.takeIf { it.isRecent() }?.let { throw it.shared() }
            try {
                return fetch().also { cached = it }.value
            } catch (e: ServiceTokenUnavailableException) {
                lastFailure = RecentFailure(e.message.orEmpty(), clock.instant())
                throw e
            }
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
        val pending = httpClient.sendAsync(tokenRequest(), HttpResponse.BodyHandlers.ofString())
        val response =
            try {
                pending.get(maxFetchDuration.toMillis(), TimeUnit.MILLISECONDS)
            } catch (_: TimeoutException) {
                pending.cancel(true)
                throw unavailable("token endpoint did not answer within $maxFetchDuration")
            } catch (e: ExecutionException) {
                throw unavailable("token endpoint unreachable: ${e.cause?.javaClass?.simpleName}")
            } catch (_: InterruptedException) {
                pending.cancel(true)
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
        return CachedToken(token, requestedAt, requestedAt.plusSeconds(expiresIn))
    }

    private fun tokenRequest(): HttpRequest {
        val credentials = "${formEncode(clientId)}:${formEncode(clientSecret)}"
        val form = "grant_type=client_credentials&scope=${formEncode(scope)}"
        return HttpRequest
            .newBuilder(tokenUri)
            .timeout(readTimeout)
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
        issuedAt: Instant,
        expiresAt: Instant,
    ) {
        private val renewAt = expiresAt.minus(minOf(RENEW_BEFORE_EXPIRY, Duration.between(issuedAt, expiresAt).dividedBy(2)))

        fun isFresh() = clock.instant().isBefore(renewAt)
    }

    private inner class RecentFailure(
        private val reason: String,
        private val at: Instant,
    ) {
        fun isRecent() = clock.instant().isBefore(at.plus(FAILURE_BACKOFF))

        fun shared() = ServiceTokenUnavailableException("token endpoint failed less than $FAILURE_BACKOFF ago: $reason")
    }

    companion object {
        val RENEW_BEFORE_EXPIRY: Duration = Duration.ofSeconds(30)
        val FAILURE_BACKOFF: Duration = Duration.ofSeconds(1)
        val DEFAULT_CONNECT_TIMEOUT: Duration = Duration.ofMillis(500)
        val DEFAULT_READ_TIMEOUT: Duration = Duration.ofMillis(1500)
        private val log = LoggerFactory.getLogger(ServiceTokenClient::class.java)
    }
}

/** No machine token could be obtained; an [IOException] so HTTP clients treat it as a network failure. */
class ServiceTokenUnavailableException(
    message: String,
) : IOException(message)
