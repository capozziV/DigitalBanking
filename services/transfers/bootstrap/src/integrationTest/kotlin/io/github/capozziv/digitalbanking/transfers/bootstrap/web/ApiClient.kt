package io.github.capozziv.digitalbanking.transfers.bootstrap.web

import org.assertj.core.api.Assertions.assertThat
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

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

/** Plain HTTP client for the integration tests: never throws on 4xx/5xx, returns status, headers and JSON. */
class ApiClient(
    port: Int,
) {
    private val mapper = JsonMapper.builder().build()

    private val client =
        RestClient
            .builder()
            .baseUrl("http://localhost:$port")
            .defaultStatusHandler({ true }) { _, _ -> }
            .build()

    fun send(
        method: HttpMethod,
        path: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): ApiResponse {
        val spec = client.method(method).uri(path).headers { h -> headers.forEach { (k, v) -> h.add(k, v) } }
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
