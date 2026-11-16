package io.github.capozziv.digitalbanking.security

import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.core.Ordered
import org.springframework.http.ProblemDetail

/**
 * The error responses every operation of a service shares, so the controllers only describe their own:
 * - 401 and 403 from this library and 500 internal-error from the service's generic handler, added to every
 *   operation unless the controller declared them with its own description;
 * - every 4xx and 5xx response is a Problem Details (`application/problem+json`, schema `ProblemDetail`), so the
 *   controllers declare only the status and the description. Any other content of an error response is replaced:
 *   for an annotation without content, springdoc fills in the method's return type (the success body), which an
 *   error never carries.
 *
 * Registered by [ApiDocsAutoConfiguration] when springdoc is on the classpath. Runs before the customizers left at the
 * lowest precedence (springdoc applies them in order), so the one of `X-Correlation-Id` sees the responses added here.
 */
class ProblemResponsesOpenApiCustomizer :
    OpenApiCustomizer,
    Ordered {
    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE - 100

    override fun customise(openApi: OpenAPI) {
        val components = openApi.components ?: Components().also { openApi.components = it }
        ModelConverters.getInstance().readAll(ProblemDetail::class.java).forEach { (name, schema) ->
            if (components.schemas?.containsKey(name) != true) components.addSchemas(name, schema)
        }
        openApi.paths.orEmpty().values.flatMap { it.readOperations() }.forEach { operation ->
            val responses = operation.responses
            COMMON.forEach { (status, description) ->
                if (!responses.containsKey(status)) responses.addApiResponse(status, ApiResponse().description(description))
            }
            responses.forEach { (status, response) ->
                if (status.isError() && response.content?.containsKey(PROBLEM_JSON) != true) response.content(problemContent())
            }
        }
    }

    private fun String.isError() = startsWith("4") || startsWith("5")

    private fun problemContent() =
        Content().addMediaType(PROBLEM_JSON, MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/ProblemDetail")))

    private companion object {
        const val PROBLEM_JSON = "application/problem+json"

        val COMMON =
            mapOf(
                "401" to "/problems/unauthorized: no bearer token, or an invalid or expired one",
                "403" to "/problems/forbidden: the token is valid, but its caller may not call this operation",
                "500" to "/problems/internal-error: unexpected error; the detail does not expose the cause",
            )
    }
}
