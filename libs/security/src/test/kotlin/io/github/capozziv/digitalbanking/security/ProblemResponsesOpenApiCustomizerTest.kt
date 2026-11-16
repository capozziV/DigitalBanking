package io.github.capozziv.digitalbanking.security

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.WebApplicationContextRunner

class ProblemResponsesOpenApiCustomizerTest {
    private val customizer = ProblemResponsesOpenApiCustomizer()

    @Test
    fun `every operation gets 401, 403 and 500 as Problem Details`() {
        val openApi = openApi("/a" to PathItem().get(operation()).post(operation()))

        customizer.customise(openApi)

        for (operation in openApi.paths["/a"]!!.readOperations()) {
            assertThat(operation.responses["401"]!!.description).contains("/problems/unauthorized")
            assertThat(operation.responses["403"]!!.description).contains("/problems/forbidden")
            assertThat(operation.responses["500"]!!.description).contains("/problems/internal-error")
            for (status in listOf("401", "403", "500")) {
                assertThat(problemSchemaRef(operation.responses[status]!!)).describedAs(status).isEqualTo(PROBLEM_REF)
            }
        }
    }

    @Test
    fun `an error response the controller declared keeps its description and gets the Problem Details content`() {
        val operation =
            operation(
                "404" to ApiResponse().description("/problems/account-not-found"),
                "403" to ApiResponse().description("/problems/forbidden: only admins"),
            )
        val openApi = openApi("/a" to PathItem().get(operation))

        customizer.customise(openApi)

        assertThat(operation.responses["404"]!!.description).isEqualTo("/problems/account-not-found")
        assertThat(problemSchemaRef(operation.responses["404"]!!)).isEqualTo(PROBLEM_REF)
        assertThat(operation.responses["403"]!!.description).isEqualTo("/problems/forbidden: only admins")
    }

    @Test
    fun `success responses and error responses with their own content are left alone`() {
        val ownContent = Content().addMediaType("text/plain", MediaType())
        val operation =
            operation(
                "200" to ApiResponse().description("ok"),
                "409" to ApiResponse().description("conflict").content(ownContent),
            )

        customizer.customise(openApi("/a" to PathItem().get(operation)))

        assertThat(operation.responses["200"]!!.content).isNull()
        assertThat(operation.responses["409"]!!.content).isSameAs(ownContent)
    }

    @Test
    fun `the ProblemDetail schema is in the components`() {
        val openApi = openApi("/a" to PathItem().get(operation()))

        customizer.customise(openApi)

        val problem = openApi.components.schemas["ProblemDetail"]!!
        assertThat(problem.properties.keys).contains("type", "title", "status", "detail", "instance")
    }

    @Test
    fun `the customizer is auto-configured in servlet applications with springdoc`() {
        val runner = WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(ApiDocsAutoConfiguration::class.java))

        runner.run { context -> assertThat(context).hasSingleBean(ProblemResponsesOpenApiCustomizer::class.java) }
        runner
            .withClassLoader(FilteredClassLoader(OpenApiCustomizer::class.java))
            .run { context -> assertThat(context).doesNotHaveBean(ProblemResponsesOpenApiCustomizer::class.java) }
    }

    private fun openApi(vararg paths: Pair<String, PathItem>): OpenAPI {
        val all = Paths()
        paths.forEach { (path, item) -> all.addPathItem(path, item) }
        return OpenAPI().paths(all)
    }

    private fun operation(vararg responses: Pair<String, ApiResponse>) =
        Operation().responses(ApiResponses().apply { responses.forEach { (status, r) -> addApiResponse(status, r) } })

    private fun problemSchemaRef(response: ApiResponse): String? =
        response.content
            ?.get("application/problem+json")
            ?.schema
            ?.`$ref`

    private companion object {
        const val PROBLEM_REF = "#/components/schemas/ProblemDetail"
    }
}
