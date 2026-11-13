package io.github.capozziv.digitalbanking.security

import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Against a fake token endpoint (and a fake peer) on a real HTTP server. */
class ServiceTokenClientIntegrationTest {
    private val tokenRequests = CopyOnWriteArrayList<TokenRequest>()
    private val peerAuthorizations = CopyOnWriteArrayList<String?>()
    private val issued = AtomicInteger()

    @Volatile private var tokenStatus = 200

    @Volatile private var expiresIn = 300

    @Volatile private var tokenDelay = Duration.ZERO

    private val server =
        HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            executor = Executors.newFixedThreadPool(8)
            createContext("/token") { exchange ->
                val form =
                    exchange.requestBody
                        .readAllBytes()
                        .decodeToString()
                        .split('&')
                        .associate { it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), Charsets.UTF_8) }
                tokenRequests += TokenRequest(exchange.requestHeaders.getFirst("Authorization"), form)
                Thread.sleep(tokenDelay.toMillis())
                val body =
                    if (tokenStatus == 200) {
                        """{"access_token":"token-${issued.incrementAndGet()}","token_type":"Bearer","expires_in":$expiresIn}"""
                    } else {
                        """{"error":"invalid_client"}"""
                    }
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(tokenStatus, body.length.toLong())
                exchange.responseBody.use { it.write(body.toByteArray()) }
            }
            createContext("/peer") { exchange ->
                peerAuthorizations += exchange.requestHeaders.getFirst("Authorization")
                exchange.sendResponseHeaders(204, -1)
                exchange.close()
            }
            start()
        }

    private val baseUrl = "http://127.0.0.1:${server.address.port}"
    private val clock = MutableClock(Instant.parse("2026-11-13T12:00:00Z"))

    private fun client(tokenUri: URI = URI.create("$baseUrl/token")) =
        ServiceTokenClient(
            tokenUri = tokenUri,
            clientId = "transfers",
            clientSecret = "s3cret",
            connectTimeout = Duration.ofMillis(300),
            readTimeout = Duration.ofMillis(700),
            clock = clock,
        )

    @AfterEach
    fun stop() = server.stop(0)

    @Test
    fun `the token is requested with client credentials, basic auth and the internal scope`() {
        val token = client().accessToken()

        assertThat(token).isEqualTo("token-1")
        val request = tokenRequests.single()
        assertThat(request.authorization).isEqualTo("Basic " + Base64.getEncoder().encodeToString("transfers:s3cret".toByteArray()))
        assertThat(request.form).containsEntry("grant_type", "client_credentials").containsEntry("scope", "digitalbanking/internal")
    }

    @Test
    fun `the cached token avoids a second call to the token endpoint`() {
        val client = client()

        client.accessToken()
        clock.advance(Duration.ofSeconds(269))
        val second = client.accessToken()

        assertThat(second).isEqualTo("token-1")
        assertThat(tokenRequests).hasSize(1)
    }

    @Test
    fun `a token is renewed 30 seconds before it expires`() {
        val client = client()

        client.accessToken()
        clock.advance(Duration.ofSeconds(270))

        assertThat(client.accessToken()).isEqualTo("token-2")
        assertThat(tokenRequests).hasSize(2)
    }

    @Test
    fun `concurrent first calls share a single token request`() {
        tokenDelay = Duration.ofMillis(200)
        val client = client()
        val threads = 8
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(threads)
        val results = (1..threads).map { pool.submit<String> { start.await().let { client.accessToken() } } }

        start.countDown()

        assertThat(results.map { it.get(5, TimeUnit.SECONDS) }).containsOnly("token-1")
        assertThat(tokenRequests).hasSize(1)
        pool.shutdown()
    }

    @Test
    fun `a short-lived token is cached for half of its validity`() {
        expiresIn = 20
        val client = client()

        client.accessToken()
        clock.advance(Duration.ofSeconds(9))
        assertThat(client.accessToken()).isEqualTo("token-1")
        clock.advance(Duration.ofSeconds(1))

        assertThat(client.accessToken()).isEqualTo("token-2")
        assertThat(tokenRequests).hasSize(2)
    }

    @Test
    fun `the worst case of a token request is the connect timeout plus the read timeout`() {
        assertThat(client().maxFetchDuration).isEqualTo(Duration.ofSeconds(1))
        assertThat(ServiceTokenClient.DEFAULT_CONNECT_TIMEOUT.plus(ServiceTokenClient.DEFAULT_READ_TIMEOUT))
            .isLessThanOrEqualTo(Duration.ofSeconds(2))
    }

    @Test
    fun `with the token endpoint down, threads waiting for the same request share its failure`() {
        tokenStatus = 503
        tokenDelay = Duration.ofMillis(300)
        val client = client()
        val threads = 8
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(threads)
        val results = (1..threads).map { pool.submit<Any> { start.await().let { runCatching { client.accessToken() }.exceptionOrNull() } } }

        start.countDown()

        assertThat(results.map { it.get(5, TimeUnit.SECONDS) }).allMatch { it is ServiceTokenUnavailableException }
        assertThat(tokenRequests).hasSize(1)
        pool.shutdown()
    }

    @Test
    fun `with the token endpoint hanging, queued threads wait about one timeout, not one each`() {
        tokenDelay = Duration.ofSeconds(5)
        val client = client()
        val threads = 4
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(threads)
        val results = (1..threads).map { pool.submit<Any> { start.await().let { runCatching { client.accessToken() }.exceptionOrNull() } } }

        val startedAt = System.nanoTime()
        start.countDown()
        val failures = results.map { it.get(10, TimeUnit.SECONDS) }
        val elapsed = Duration.ofNanos(System.nanoTime() - startedAt)

        assertThat(failures).allMatch { it is ServiceTokenUnavailableException }
        assertThat(tokenRequests).hasSize(1)
        assertThat(elapsed).isLessThan(client.maxFetchDuration.multipliedBy(2))
        pool.shutdown()
    }

    @Test
    fun `right after a failure the next call fails fast, and the endpoint is tried again a second later`() {
        tokenStatus = 503
        val client = client()
        assertThatThrownBy { client.accessToken() }.isInstanceOf(ServiceTokenUnavailableException::class.java)
        tokenStatus = 200

        assertThatThrownBy { client.accessToken() }.isInstanceOf(ServiceTokenUnavailableException::class.java)
        assertThat(tokenRequests).hasSize(1)

        clock.advance(ServiceTokenClient.FAILURE_BACKOFF)
        assertThat(client.accessToken()).isEqualTo("token-1")
        assertThat(tokenRequests).hasSize(2)
    }

    @Test
    fun `an error from the token endpoint is an IOException that does not leak the secret`() {
        tokenStatus = 401

        assertThatThrownBy { client().accessToken() }
            .isInstanceOf(IOException::class.java)
            .isInstanceOf(ServiceTokenUnavailableException::class.java)
            .hasMessageNotContaining("s3cret")
    }

    @Test
    fun `a token endpoint that does not answer in time is an IOException`() {
        tokenDelay = Duration.ofSeconds(3)

        assertThatThrownBy { client().accessToken() }.isInstanceOf(ServiceTokenUnavailableException::class.java)
    }

    @Test
    fun `the interceptor sends the bearer token to the peer`() {
        val restClient =
            RestClient
                .builder()
                .baseUrl(baseUrl)
                .requestInterceptor(client().interceptor())
                .build()

        restClient
            .get()
            .uri("/peer")
            .retrieve()
            .toBodilessEntity()
        restClient
            .get()
            .uri("/peer")
            .retrieve()
            .toBodilessEntity()

        assertThat(peerAuthorizations).containsExactly("Bearer token-1", "Bearer token-1")
        assertThat(tokenRequests).hasSize(1)
    }

    @Test
    fun `without a token the call fails like an unreachable peer, so gateways answer 503`() {
        val unreachable = client(URI.create("http://127.0.0.1:1/token"))
        val restClient =
            RestClient
                .builder()
                .baseUrl(baseUrl)
                .requestInterceptor(unreachable.interceptor())
                .build()

        assertThatThrownBy {
            restClient
                .get()
                .uri("/peer")
                .retrieve()
                .toBodilessEntity()
        }.isInstanceOf(ResourceAccessException::class.java)
        assertThat(peerAuthorizations).isEmpty()
    }

    private data class TokenRequest(
        val authorization: String?,
        val form: Map<String, String>,
    )

    private class MutableClock(
        @Volatile private var now: Instant,
    ) : Clock() {
        fun advance(duration: Duration) {
            now = now.plus(duration)
        }

        override fun instant(): Instant = now

        override fun getZone() = ZoneOffset.UTC

        override fun withZone(zone: java.time.ZoneId?) = this
    }
}
