package io.github.capozziv.digitalbanking.observability

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.HeaderParameter
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.core.Ordered

/**
 * Documents what [CorrelationIdFilter] does on every operation of a service's OpenAPI contract: the optional
 * `X-Correlation-Id` request header and the same header on every response. Registered by
 * [ObservabilityAutoConfiguration] when springdoc is on the classpath; a header the controller declares itself wins.
 * Runs last (springdoc applies the customizers in order), so the responses other customizers add, such as the 401,
 * 403 and 500 of `:libs:security`, also get the header.
 */
class CorrelationIdOpenApiCustomizer :
    OpenApiCustomizer,
    Ordered {
    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE

    override fun customise(openApi: OpenAPI) {
        openApi.paths.orEmpty().values.flatMap { it.readOperations() }.forEach { operation ->
            val declared =
                operation.parameters.orEmpty().any {
                    it.`in` == "header" &&
                        it.name.equals(CorrelationId.HEADER, ignoreCase = true)
                }
            if (!declared) operation.addParametersItem(requestHeader())
            operation.responses.orEmpty().values.forEach { response ->
                if (response.headers?.containsKey(CorrelationId.HEADER) != true) {
                    response.addHeaderObject(CorrelationId.HEADER, responseHeader())
                }
            }
        }
    }

    private fun requestHeader() =
        HeaderParameter()
            .name(CorrelationId.HEADER)
            .required(false)
            .description(
                "Correlation id of the request, echoed in the response and propagated to the services and events it " +
                    "reaches. Reused when it has 1 to 128 letters, digits or `. _ - = + /`; generated (a UUID) when " +
                    "absent or invalid.",
            ).schema(StringSchema().maxLength(MAX_LENGTH).example("0f8fad5b-d9cb-469f-a165-70867728950e"))

    private fun responseHeader() =
        Header()
            .description("Correlation id of the request: the one received, or the one generated for it")
            .schema(StringSchema())

    private companion object {
        const val MAX_LENGTH = 128
    }
}
