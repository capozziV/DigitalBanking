package io.github.capozziv.digitalbanking.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

/** The documentation routes are one rule of this library: open while springdoc serves them, 404 once it is off. */
class ApiDocsIntegrationTest {
    @Nested
    @SpringBootTest(classes = [SecurityTestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    inner class Enabled {
        @LocalServerPort
        private var port: Int = 0

        @Test
        fun `the contract in JSON and YAML and the Swagger UI are open without a token`() {
            val http = TestHttp(port)

            for (path in DOCS_ROUTES - SWAGGER_UI_REDIRECT) {
                assertThat(http.get(path).statusCode()).describedAs(path).isEqualTo(200)
            }
            assertThat(http.get(SWAGGER_UI_REDIRECT).statusCode()).isEqualTo(302)
        }
    }

    @Nested
    @SpringBootTest(
        classes = [SecurityTestApplication::class],
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = ["springdoc.api-docs.enabled=false", "springdoc.swagger-ui.enabled=false"],
    )
    inner class Disabled {
        @LocalServerPort
        private var port: Int = 0

        @Test
        fun `with springdoc off the documentation routes are 404, not 401`() {
            val http = TestHttp(port)

            for (path in DOCS_ROUTES) {
                assertThat(http.get(path).statusCode()).describedAs(path).isEqualTo(404)
            }
        }
    }

    private companion object {
        const val SWAGGER_UI_REDIRECT = "/swagger-ui.html"
        val DOCS_ROUTES = listOf("/v3/api-docs", "/v3/api-docs.yaml", "/swagger-ui/index.html", SWAGGER_UI_REDIRECT)
    }
}
