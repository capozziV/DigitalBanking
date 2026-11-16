package io.github.capozziv.digitalbanking.testing.security

import org.assertj.core.api.Assertions.assertThat
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnumerablePropertySource
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * What every service's OpenAPI contract shares (from `:libs:security` and `:libs:observabilidade`), checked the same
 * way in each service:
 * - [assertOpenWithoutToken]: the contract in JSON and YAML and the Swagger UI answer without a token;
 * - [assertCommonContract]: every operation documents `X-Correlation-Id` and 401, 403 and 500, and every error
 *   response is a Problem Details;
 * - [assertAwsProfile]: with the `aws` profile the documentation routes are 404 and `application-aws.yml` turns
 *   springdoc off and points no AWS endpoint to LocalStack.
 */
object ApiDocsAssertions {
    private const val CORRELATION_ID = "X-Correlation-Id"
    private const val PROBLEM_JSON = "application/problem+json"
    private val ROUTES = listOf("/v3/api-docs", "/v3/api-docs.yaml", "/swagger-ui/index.html")
    private val client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()
    private val mapper = JsonMapper.builder().build()

    /** The contract (JSON), read without a token. */
    fun fetchContract(port: Int): JsonNode {
        val response = get(port, "/v3/api-docs")
        assertThat(response.statusCode()).describedAs(response.body()).isEqualTo(200)
        return mapper.readTree(response.body())
    }

    fun assertOpenWithoutToken(port: Int) {
        for (path in ROUTES) {
            assertThat(get(port, path).statusCode()).describedAs(path).isEqualTo(200)
        }
    }

    fun assertCommonContract(docs: JsonNode) {
        val paths = docs.get("paths")
        val operations =
            paths.propertyNames().flatMap { path ->
                paths.get(path).propertyNames().map { method -> "${method.uppercase()} $path" to paths.get(path).get(method) }
            }
        assertThat(operations).isNotEmpty()
        assertThat(docs.get("components").get("schemas").get("ProblemDetail")).isNotNull()
        for ((name, operation) in operations) {
            val header =
                operation.get("parameters")?.singleOrNull {
                    it.get("in").asString() == "header" &&
                        it.get("name").asString() == CORRELATION_ID
                }
            assertThat(header).describedAs("$name: $CORRELATION_ID parameter").isNotNull()
            assertThat(header!!.get("required")?.asBoolean() ?: false).describedAs(name).isFalse()
            val responses = operation.get("responses")
            assertThat(responses.get("401")?.get("description")?.asString()).describedAs("$name 401").contains("/problems/unauthorized")
            assertThat(responses.get("403")?.get("description")?.asString()).describedAs("$name 403").contains("/problems/forbidden")
            assertThat(responses.get("500")?.get("description")?.asString()).describedAs("$name 500").contains("/problems/internal-error")
            for (status in responses.propertyNames()) {
                val response = responses.get(status)
                assertThat(response.get("headers")?.get(CORRELATION_ID)).describedAs("$name $status: $CORRELATION_ID header").isNotNull()
                if (status.startsWith("4") || status.startsWith("5")) {
                    assertThat(
                        response
                            .get("content")
                            ?.get(PROBLEM_JSON)
                            ?.get("schema")
                            ?.get("\$ref")
                            ?.asString(),
                    ).describedAs("$name $status: Problem Details").isEqualTo("#/components/schemas/ProblemDetail")
                }
            }
        }
    }

    fun assertAwsProfile(
        port: Int,
        environment: ConfigurableEnvironment,
    ) {
        assertThat(environment.activeProfiles).contains("aws")
        for (path in ROUTES) {
            assertThat(get(port, path).statusCode()).describedAs(path).isEqualTo(404)
        }
        val awsFile =
            environment.propertySources
                .filterIsInstance<EnumerablePropertySource<*>>()
                .singleOrNull { it.name.contains("application-aws.yml") }
        assertThat(awsFile).describedAs("application-aws.yml loaded").isNotNull()
        assertThat(awsFile!!.getProperty("springdoc.api-docs.enabled")).isEqualTo(false)
        assertThat(awsFile.getProperty("springdoc.swagger-ui.enabled")).isEqualTo(false)
        for (name in awsFile.propertyNames) {
            assertThat(awsFile.getProperty(name).toString()).describedAs(name).doesNotContain("localhost").doesNotContain("4566")
        }
    }

    private fun get(
        port: Int,
        path: String,
    ): HttpResponse<String> =
        client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:$port$path")).GET().build(), HttpResponse.BodyHandlers.ofString())
}
