package io.github.capozziv.digitalbanking.observability

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.parameters.HeaderParameter
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.WebApplicationContextRunner

class CorrelationIdOpenApiCustomizerTest {
    private val customizer = CorrelationIdOpenApiCustomizer()

    @Test
    fun `every operation accepts the optional X-Correlation-Id header and returns it on every response`() {
        val get = operation("200", "404")
        val post = operation("201", "400")
        val openApi = OpenAPI().paths(Paths().addPathItem("/a", PathItem().get(get).post(post)))

        customizer.customise(openApi)

        for (operation in listOf(get, post)) {
            val header = operation.parameters.single { it.`in` == "header" && it.name == CorrelationId.HEADER }
            assertThat(header.required).isFalse()
            assertThat(header.description).contains("generated")
            for ((status, response) in operation.responses) {
                assertThat(response.headers[CorrelationId.HEADER]).describedAs(status).isNotNull()
            }
        }
    }

    @Test
    fun `a header the controller already declared is not repeated`() {
        val operation = operation("200").addParametersItem(HeaderParameter().name(CorrelationId.HEADER).description("own"))

        customizer.customise(OpenAPI().paths(Paths().addPathItem("/a", PathItem().get(operation))))

        assertThat(operation.parameters.filter { it.name == CorrelationId.HEADER }.map { it.description }).containsExactly("own")
    }

    @Test
    fun `the customizer is auto-configured in servlet applications with springdoc`() {
        val runner = WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration::class.java))

        runner.run { context -> assertThat(context).hasSingleBean(CorrelationIdOpenApiCustomizer::class.java) }
        runner
            .withClassLoader(FilteredClassLoader(OpenApiCustomizer::class.java))
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).doesNotHaveBean(CorrelationIdOpenApiCustomizer::class.java)
            }
    }

    private fun operation(vararg statuses: String) =
        Operation().responses(ApiResponses().apply { statuses.forEach { addApiResponse(it, ApiResponse().description(it)) } })
}
