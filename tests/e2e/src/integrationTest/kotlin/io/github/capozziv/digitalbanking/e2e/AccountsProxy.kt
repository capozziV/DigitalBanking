package io.github.capozziv.digitalbanking.e2e

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * HTTP proxy between `transfers` and `accounts`, so a test can stop the saga at an exact call.
 *
 * Every request is forwarded to [targetUrl] as is. A [Hold] made by [holdNext] keeps the next matching request
 * waiting until the test [Hold.release]s it (forwarded then, to whatever `accounts` is at that moment) or
 * [Hold.drop]s it (answered `502` without reaching `accounts`). When `accounts` is not reachable the proxy answers
 * `502`, as a gateway in front of a stopped service does; `transfers` treats both as a transient failure.
 */
class AccountsProxy(
    private val targetUrl: String,
) : AutoCloseable {
    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val client: HttpClient =
        HttpClient
            .newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(1))
            .build()
    private val holds = CopyOnWriteArrayList<Hold>()

    val baseUrl: String get() = "http://127.0.0.1:${server.address.port}"

    init {
        server.executor = Executors.newVirtualThreadPerTaskExecutor()
        server.createContext("/") { exchange -> exchange.use { handle(it) } }
        server.start()
    }

    /** Holds the next request with [method] and exactly [path] (query excluded). */
    fun holdNext(
        method: String,
        path: String,
    ): Hold = Hold(method, path).also(holds::add)

    /** Lets every pending hold go, so a failed test does not leave a request stuck for the next one. */
    fun releaseAll() {
        holds.forEach { it.release() }
        holds.clear()
    }

    override fun close() {
        releaseAll()
        server.stop(0)
    }

    private fun handle(exchange: HttpExchange) {
        val method = exchange.requestMethod
        val path = exchange.requestURI.rawPath
        val idempotencyKey = exchange.requestHeaders.getFirst("Idempotency-Key")
        val body = exchange.requestBody.readAllBytes()
        val hold = holds.firstOrNull { it.claim(method, path, idempotencyKey) }
        val response = if (hold == null || hold.awaitDecision()) forward(exchange, method, body) else null
        // Recorded before answering: the client that sent a held request may be gone by now (killed process).
        hold?.forwarded?.complete(response?.statusCode() ?: BAD_GATEWAY)
        if (response == null) {
            exchange.respond(BAD_GATEWAY, ByteArray(0), emptyMap())
        } else {
            val headers = response.headers().map().filterKeys { it.lowercase() !in RESTRICTED_HEADERS && !it.startsWith(":") }
            exchange.respond(response.statusCode(), response.body(), headers)
        }
    }

    /** The response of `accounts`, or null when it cannot be reached. */
    private fun forward(
        exchange: HttpExchange,
        method: String,
        body: ByteArray,
    ): HttpResponse<ByteArray>? {
        val publisher = if (body.isEmpty()) HttpRequest.BodyPublishers.noBody() else HttpRequest.BodyPublishers.ofByteArray(body)
        val request =
            HttpRequest
                .newBuilder(URI.create(targetUrl + exchange.requestURI.toString()))
                .timeout(FORWARD_TIMEOUT)
                .method(method, publisher)
        exchange.requestHeaders
            .filterKeys { it.lowercase() !in RESTRICTED_HEADERS }
            .forEach { (name, values) -> values.forEach { request.header(name, it) } }
        return runCatching { client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray()) }.getOrNull()
    }

    private fun HttpExchange.respond(
        status: Int,
        body: ByteArray,
        headers: Map<String, List<String>>,
    ) {
        headers.forEach { (name, values) -> responseHeaders[name] = values }
        sendResponseHeaders(status, if (body.isEmpty()) -1 else body.size.toLong())
        if (body.isNotEmpty()) responseBody.write(body)
    }

    /** A request held at the proxy, as seen by the test. */
    data class HeldRequest(
        val method: String,
        val path: String,
        val idempotencyKey: String?,
    ) {
        /** The id in the saga's key `transfer-<id>-<step>`. */
        fun transferId(): String =
            checkNotNull(idempotencyKey) { "held request $method $path has no Idempotency-Key" }
                .removePrefix("transfer-")
                .substringBeforeLast('-')
    }

    inner class Hold internal constructor(
        private val method: String,
        private val path: String,
    ) {
        private val held = CompletableFuture<HeldRequest>()
        private val decision = CompletableFuture<Boolean>()
        internal val forwarded = CompletableFuture<Int>()

        internal fun claim(
            requestMethod: String,
            requestPath: String,
            idempotencyKey: String?,
        ): Boolean {
            if (requestMethod != method || requestPath != path) return false
            synchronized(this) {
                if (held.isDone) return false
                held.complete(HeldRequest(requestMethod, requestPath, idempotencyKey))
            }
            return true
        }

        /** True to forward. A hold nobody decides on is released after [MAX_HOLD]. */
        internal fun awaitDecision(): Boolean = decision.completeOnTimeout(true, MAX_HOLD.toMillis(), TimeUnit.MILLISECONDS).get()

        fun awaitHeld(timeout: Duration): HeldRequest = held.get(timeout.toMillis(), TimeUnit.MILLISECONDS)

        /** Forwards the held request now. */
        fun release() {
            decision.complete(true)
        }

        /** Answers the held request with `502` without forwarding it. */
        fun drop() {
            decision.complete(false)
        }

        /** The status `accounts` gave to the released request (`502` when dropped or unreachable). */
        fun awaitForwarded(timeout: Duration): Int = forwarded.get(timeout.toMillis(), TimeUnit.MILLISECONDS)
    }

    private companion object {
        const val BAD_GATEWAY = 502

        /** Set by the HTTP clients themselves; copying them breaks the request or the response framing. */
        val RESTRICTED_HEADERS = setOf("host", "connection", "content-length", "expect", "upgrade", "transfer-encoding", "keep-alive")

        val FORWARD_TIMEOUT: Duration = Duration.ofSeconds(10)

        /** A forgotten hold never blocks a request forever. */
        val MAX_HOLD: Duration = Duration.ofSeconds(60)
    }
}
