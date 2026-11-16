package io.github.capozziv.digitalbanking.transfers.bootstrap.web

import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.http.HttpClient

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
 * Every request carries `Authorization: Bearer` [token] (by default a customer token of
 * [TransfersIntegrationTest.CUSTOMER_ID]) unless [token] is null or the request sets its own `Authorization`.
 */
class ApiClient(
    private val port: Int,
    private val token: String? = JwtTestSupport.customerToken(TransfersIntegrationTest.CUSTOMER_ID),
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

    /** The same client with another bearer token (null: no token). */
    fun withToken(token: String?) = ApiClient(port, token)

    fun send(
        method: HttpMethod,
        path: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): ApiResponse {
        val spec =
            client.method(method).uri(path).headers { h ->
                if (token != null && headers.keys.none { it.equals(HttpHeaders.AUTHORIZATION, ignoreCase = true) }) {
                    h.setBearerAuth(token)
                }
                headers.forEach { (k, v) -> h.add(k, v) }
            }
        if (body != null) spec.contentType(MediaType.APPLICATION_JSON).body(body)
        val entity = spec.retrieve().toEntity(String::class.java)
        val raw = entity.body
        // JSON bodies only (application/json, application/problem+json); YAML or text stays in rawBody.
        val contentType = entity.headers.contentType
        val json = contentType != null && contentType.subtype.endsWith("json")
        return ApiResponse(
            status = entity.statusCode.value(),
            headers = entity.headers,
            rawBody = raw,
            body = raw?.takeIf { json && it.isNotBlank() }?.let { mapper.readTree(it) },
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
}

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
