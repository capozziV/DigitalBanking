package io.github.capozziv.digitalbanking.security

import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.test.web.server.LocalServerPort
import java.time.Duration
import java.util.UUID

@SpringBootTest(
    classes = [SecurityTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["management.endpoints.web.exposure.include=health,metrics"],
)
@ExtendWith(OutputCaptureExtension::class)
class SecurityAutoConfigurationIntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    private val http by lazy { TestHttp(port) }
    private val userId = UUID.fromString("00000000-0000-7000-8000-000000000001")

    @Test
    fun `no token is 401 unauthorized as Problem Details, with the Bearer challenge`() {
        val response = http.get("/caller")

        assertProblem(response, 401, "unauthorized")
        assertThat(response.headers().firstValue("WWW-Authenticate")).hasValueSatisfying { assertThat(it).startsWith("Bearer") }
    }

    @Test
    fun `401 and 403 carry the correlation id of the request`() {
        val correlation = mapOf("X-Correlation-Id" to "corr-401-403")

        val unauthorized = http.get("/caller", headers = correlation)
        val forbidden = http.get("/admin/ping", JwtTestSupport.customerToken(userId), correlation)

        assertProblem(unauthorized, 401, "unauthorized")
        assertThat(unauthorized.headers().firstValue("X-Correlation-Id")).hasValue("corr-401-403")
        assertProblem(forbidden, 403, "forbidden")
        assertThat(forbidden.headers().firstValue("X-Correlation-Id")).hasValue("corr-401-403")
    }

    @Test
    fun `an expired token is 401`() {
        assertProblem(http.get("/caller", JwtTestSupport.token(userId.toString(), expiresIn = Duration.ofMinutes(-5))), 401, "unauthorized")
    }

    @Test
    fun `a token from another issuer is 401`() {
        assertProblem(http.get("/caller", JwtTestSupport.token(userId.toString(), issuer = "http://other.test")), 401, "unauthorized")
    }

    @Test
    fun `a malformed token is 401`() {
        assertProblem(http.get("/caller", "not-a-jwt"), 401, "unauthorized")
    }

    @Test
    fun `a user token whose sub is not a UUID is 401`() {
        assertProblem(http.get("/caller", JwtTestSupport.token("not-a-uuid")), 401, "unauthorized")
    }

    @Test
    fun `the controller reads the caller resolved from the token`() {
        assertThat(http.get("/caller", JwtTestSupport.customerToken(userId)).body()).isEqualTo("customer $userId")
        assertThat(http.get("/caller", JwtTestSupport.adminToken(userId)).body()).isEqualTo("admin $userId")
        assertThat(http.get("/caller", JwtTestSupport.serviceToken()).body()).isEqualTo("service")
    }

    @Test
    fun `admin routes are 403 forbidden without the admin group and 200 with it`() {
        assertProblem(http.get("/admin/ping", JwtTestSupport.customerToken(userId)), 403, "forbidden")
        assertProblem(http.get("/admin/ping", JwtTestSupport.serviceToken()), 403, "forbidden")
        assertThat(http.get("/admin/ping", JwtTestSupport.adminToken(userId)).statusCode()).isEqualTo(200)
    }

    @Test
    fun `internal routes are 403 for a customer token and 200 for a service token`() {
        assertProblem(http.get("/internal/ping", JwtTestSupport.customerToken(userId)), 403, "forbidden")
        assertThat(http.get("/internal/ping", JwtTestSupport.serviceToken()).statusCode()).isEqualTo(200)
    }

    @Test
    fun `a user token that carries the internal scope is 401 on internal routes, not a service`() {
        val adminWithInternalScope =
            JwtTestSupport.token(userId.toString(), groups = listOf("admin"), scopes = listOf("digitalbanking/internal"))
        val customerWithInternalScope = JwtTestSupport.token(userId.toString(), scopes = listOf("digitalbanking/internal"))

        assertProblem(http.get("/internal/ping", adminWithInternalScope), 401, "unauthorized")
        assertProblem(http.get("/internal/ping", customerWithInternalScope), 401, "unauthorized")
        assertProblem(http.get("/caller", adminWithInternalScope), 401, "unauthorized")
    }

    @Test
    fun `admin routes without a token are 401, not 403`() {
        assertProblem(http.get("/admin/ping"), 401, "unauthorized")
    }

    @Test
    fun `the service declares its own rules`() {
        assertThat(http.get("/customer-only", JwtTestSupport.customerToken(userId)).statusCode()).isEqualTo(200)
        assertThat(http.get("/customer-only", JwtTestSupport.adminToken(userId)).statusCode()).isEqualTo(200)
        assertProblem(http.get("/customer-only", JwtTestSupport.serviceToken()), 403, "forbidden")
    }

    @Test
    fun `a route no rule declares is denied even with a valid token`() {
        assertProblem(http.get("/not-declared", JwtTestSupport.adminToken(userId)), 403, "forbidden")
    }

    @Test
    fun `the health endpoint is open and the rest of the actuator is for admins`() {
        assertThat(http.get("/actuator/health").statusCode()).isEqualTo(200)
        assertProblem(http.get("/actuator/metrics"), 401, "unauthorized")
        assertProblem(http.get("/actuator/metrics", JwtTestSupport.customerToken(userId)), 403, "forbidden")
        assertThat(http.get("/actuator/metrics", JwtTestSupport.adminToken(userId)).statusCode()).isEqualTo(200)
    }

    @Test
    fun `reading the caller on a route without authentication is 401, not 500`() {
        assertProblem(http.get("/public/caller"), 401, "unauthorized")
    }

    @Test
    fun `a client that disconnected is neither a 500 nor an ERROR log`(output: CapturedOutput) {
        assertThat(http.get("/public/disconnect-async").statusCode()).isNotEqualTo(500)
        assertThat(http.get("/public/disconnect-tomcat").statusCode()).isNotEqualTo(500)

        assertThat(output.out).doesNotContain("unexpected error")
    }

    @Test
    fun `any other exception still reaches the service's own handler`(output: CapturedOutput) {
        assertThat(http.get("/public/boom").statusCode()).isEqualTo(500)
        assertThat(output.out).contains("unexpected error")
    }
}
