package io.github.capozziv.digitalbanking.accounts.bootstrap

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.springframework.security.oauth2.jwt.JwtException
import java.net.InetSocketAddress
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Test HTTP server that plays both the token endpoint (`POST /token`, client credentials) and the `customer`
 * service (`GET /internal/customers/{id}`), so the accounts service runs its real `ServiceTokenClient` and its
 * real HTTP gateway.
 *
 * - `/internal/customers/{id}` demands `Authorization: Bearer <service token>` (a valid token of [JwtTestSupport]
 *   with the internal scope), else 401.
 * - Every customer is ACTIVE unless [answer] sets another [Behavior] for it.
 */
class CustomerServiceStub private constructor(
    private val server: HttpServer,
) {
    sealed interface Behavior {
        data object Active : Behavior

        data object Blocked : Behavior

        data object NotFound : Behavior

        data object ServerError : Behavior

        /** Answers ACTIVE, but only after [delay]. */
        data class Slow(
            val delay: Duration,
        ) : Behavior

        /** Sends the headers of an ACTIVE answer at once, and its body only after [delay]. */
        data class SlowBody(
            val delay: Duration,
        ) : Behavior
    }

    private val behaviors = ConcurrentHashMap<UUID, Behavior>()
    private val decoder = JwtTestSupport.jwtDecoder()

    /** Every customer lookup received, with the Authorization header it carried. */
    val lookups = CopyOnWriteArrayList<Lookup>()
    val tokenRequests = AtomicInteger()

    data class Lookup(
        val customerId: String,
        val authorization: String?,
        val correlationId: String?,
    )

    val baseUrl: String get() = "http://127.0.0.1:${server.address.port}"

    fun answer(
        customerId: UUID,
        behavior: Behavior,
    ) {
        behaviors[customerId] = behavior
    }

    fun lookupsOf(customerId: UUID) = lookups.filter { it.customerId == customerId.toString() }

    private fun handleToken(exchange: HttpExchange) {
        tokenRequests.incrementAndGet()
        exchange.requestBody.readAllBytes()
        val body = """{"access_token":"${JwtTestSupport.serviceToken("accounts")}","token_type":"Bearer","expires_in":300}"""
        exchange.respond(200, body)
    }

    private fun handleCustomer(exchange: HttpExchange) {
        val id = exchange.requestURI.path.removePrefix(CUSTOMERS_PATH)
        val authorization = exchange.requestHeaders.getFirst("Authorization")
        lookups += Lookup(id, authorization, exchange.requestHeaders.getFirst("X-Correlation-Id"))
        if (!isServiceToken(authorization)) return exchange.respond(401, """{"status":401}""")
        val customerId = runCatching { UUID.fromString(id) }.getOrNull() ?: return exchange.respond(404, """{"status":404}""")
        when (val behavior = behaviors[customerId] ?: Behavior.Active) {
            Behavior.Active -> {
                exchange.respond(200, customer(customerId, "ACTIVE"))
            }

            Behavior.Blocked -> {
                exchange.respond(200, customer(customerId, "BLOCKED"))
            }

            Behavior.NotFound -> {
                exchange.respond(404, """{"type":"/problems/customer-not-found","status":404}""")
            }

            Behavior.ServerError -> {
                exchange.respond(500, """{"status":500}""")
            }

            is Behavior.Slow -> {
                Thread.sleep(behavior.delay.toMillis())
                exchange.respond(200, customer(customerId, "ACTIVE"))
            }

            is Behavior.SlowBody -> {
                exchange.respond(200, customer(customerId, "ACTIVE"), bodyDelay = behavior.delay)
            }
        }
    }

    private fun isServiceToken(authorization: String?): Boolean {
        val token = authorization?.removePrefix("Bearer ")?.takeIf { authorization.startsWith("Bearer ") } ?: return false
        return try {
            val scope = decoder.decode(token).getClaimAsString("scope").orEmpty()
            JwtTestSupport.SERVICE_SCOPE in scope.split(" ")
        } catch (_: JwtException) {
            false
        }
    }

    private fun customer(
        id: UUID,
        status: String,
    ) = """{"id":"$id","status":"$status"}"""

    private fun HttpExchange.respond(
        status: Int,
        body: String,
        bodyDelay: Duration = Duration.ZERO,
    ) {
        try {
            val bytes = body.toByteArray()
            responseHeaders.add("Content-Type", "application/json")
            sendResponseHeaders(status, bytes.size.toLong())
            if (!bodyDelay.isZero) {
                responseBody.flush()
                Thread.sleep(bodyDelay.toMillis())
            }
            responseBody.use { it.write(bytes) }
        } catch (_: java.io.IOException) {
            // The client gave up (timeout): nothing to answer.
        } finally {
            close()
        }
    }

    companion object {
        private const val CUSTOMERS_PATH = "/internal/customers/"

        /** One server for every integration test of the JVM, like the PostgreSQL container. */
        val instance: CustomerServiceStub by lazy { start() }

        private fun start(): CustomerServiceStub {
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            server.executor = Executors.newVirtualThreadPerTaskExecutor()
            val stub = CustomerServiceStub(server)
            server.createContext("/token") { stub.handleToken(it) }
            server.createContext(CUSTOMERS_PATH) { stub.handleCustomer(it) }
            server.start()
            return stub
        }
    }
}
