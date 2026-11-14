package io.github.capozziv.digitalbanking.accounts.bootstrap.web

import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.http.HttpClient
import java.util.UUID

data class ApiResponse(
    val status: Int,
    val headers: HttpHeaders,
    val rawBody: String?,
    val body: JsonNode?,
) {
    fun header(name: String): String? = headers.getFirst(name)

    /** Slug of the Problem Details type (`/problems/<slug>`), or null when the body has no type. */
    fun problemType(): String? = body?.get("type")?.asString()

    /** Fields listed in `errors` of a validation-error problem. */
    fun errorFields(): List<String> {
        val errors = body?.get("errors") ?: return emptyList()
        return (0 until errors.size()).map { errors.get(it).get("field").asString() }
    }

    /** Messages listed in `errors` of a validation-error problem. */
    fun errorMessages(): List<String> {
        val errors = body?.get("errors") ?: return emptyList()
        return (0 until errors.size()).map { errors.get(it).get("message").asString() }
    }
}

/**
 * Plain HTTP client for the integration tests: never throws on 4xx/5xx, returns status, headers and JSON.
 *
 * Every request carries `Authorization: Bearer <token>` from [defaultToken] (a fresh admin token by default, which
 * may call every route), unless the request sets its own `Authorization` header (see [bearer]). With [defaultToken]
 * null, requests without their own header go without a token.
 */
class ApiClient(
    port: Int,
    private val defaultToken: (() -> String)? = { JwtTestSupport.adminToken(ADMIN_ID) },
) {
    private val mapper = JsonMapper.builder().build()

    private val client =
        RestClient
            .builder()
            .baseUrl("http://localhost:$port")
            // The JDK client, never retrying: Apache HttpClient 5 (on the test classpath) repeats a 503 after
            // 1 s, which would send a second request and hide the timing of a single one.
            .requestFactory(JdkClientHttpRequestFactory(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()))
            .defaultStatusHandler({ true }) { _, _ -> }
            .build()

    fun send(
        method: HttpMethod,
        path: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): ApiResponse {
        val withToken =
            if (defaultToken == null || headers.keys.any { it.equals(HttpHeaders.AUTHORIZATION, ignoreCase = true) }) {
                headers
            } else {
                headers + bearer(defaultToken())
            }
        val spec = client.method(method).uri(path).headers { h -> withToken.forEach { (k, v) -> h.add(k, v) } }
        if (body != null) spec.contentType(MediaType.APPLICATION_JSON).body(body)
        val entity = spec.retrieve().toEntity(String::class.java)
        val raw = entity.body
        return ApiResponse(
            status = entity.statusCode.value(),
            headers = entity.headers,
            rawBody = raw,
            body = raw?.takeIf { it.isNotBlank() }?.let { mapper.readTree(it) },
        )
    }

    fun get(
        path: String,
        headers: Map<String, String> = emptyMap(),
    ) = send(HttpMethod.GET, path, headers = headers)

    fun post(
        path: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ) = send(HttpMethod.POST, path, body, headers)

    fun put(
        path: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ) = send(HttpMethod.PUT, path, body, headers)

    /** `POST /accounts` (no body: the customer is the caller) as [customerId], with a new opening key unless given. */
    fun openAccount(
        customerId: UUID = UUID.randomUUID(),
        idempotencyKey: String = UUID.randomUUID().toString(),
        token: String = JwtTestSupport.customerToken(customerId),
    ) = send(HttpMethod.POST, "/accounts", headers = mapOf("Idempotency-Key" to idempotencyKey) + bearer(token))

    companion object {
        /** The admin of the default token. */
        val ADMIN_ID: UUID = UUID.fromString("00000000-0000-7000-8000-0000000000ad")
    }
}

/** The `Authorization` header with [token]. */
fun bearer(token: String) = mapOf(HttpHeaders.AUTHORIZATION to "Bearer $token")

/** The `Authorization` header of a customer token of [customerId]. */
fun asCustomer(customerId: UUID) = bearer(JwtTestSupport.customerToken(customerId))

/** Asserts a Problem Details response (RFC 9457) with the given status and type `/problems/<slug>`. */
fun assertProblem(
    response: ApiResponse,
    status: Int,
    slug: String,
) {
    assertThat(response.status).describedAs(response.rawBody).isEqualTo(status)
    assertThat(response.header("Content-Type")).startsWith("application/problem+json")
    assertThat(response.problemType()).isEqualTo("/problems/$slug")
    assertThat(response.body!!.get("status").asInt()).isEqualTo(status)
}
