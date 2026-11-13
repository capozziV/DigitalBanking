package io.github.capozziv.digitalbanking.security

import org.assertj.core.api.Assertions.assertThat
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/** Plain HTTP calls to the test application, so the whole servlet filter chain runs. */
class TestHttp(
    private val port: Int,
) {
    private val client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()

    fun get(
        path: String,
        token: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): HttpResponse<String> {
        val request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:$port$path")).GET()
        if (token != null) request.header("Authorization", "Bearer $token")
        headers.forEach { (name, value) -> request.header(name, value) }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString())
    }
}

private val mapper = JsonMapper.builder().build()

fun assertProblem(
    response: HttpResponse<String>,
    status: Int,
    slug: String,
) {
    assertThat(response.statusCode()).isEqualTo(status)
    assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying { assertThat(it).startsWith("application/problem+json") }
    val body: JsonNode = mapper.readTree(response.body())
    assertThat(body["type"].asString()).isEqualTo("/problems/$slug")
    assertThat(body["status"].asInt()).isEqualTo(status)
    assertThat(body["title"].asString()).isNotBlank()
    assertThat(body["detail"].asString()).isNotBlank()
}
