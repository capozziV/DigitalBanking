package io.github.capozziv.digitalbanking.customer.bootstrap

import io.github.capozziv.digitalbanking.customer.bootstrap.web.ApiClient
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.util.UUID

/** Deny by default: a route added to a controller without a security rule would answer 403 to everyone. */
class RouteRulesIntegrationTest : CustomerIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    @Test
    fun `no route of the service answers 403 to the role it is meant for`() {
        val api = ApiClient(port)
        val sub = UUID.randomUUID()
        val admin = JwtTestSupport.adminToken(sub)
        val service = JwtTestSupport.serviceToken()
        val routes =
            handlerMapping.handlerMethods
                .filter { (_, handler) -> isOurs(handler) }
                .flatMap { (info, _) ->
                    val patterns = info.pathPatternsCondition!!.patternValues
                    info.methodsCondition.methods.flatMap { method -> patterns.map { method.asHttpMethod() to it } }
                }

        assertThat(routes).describedAs("routes found").hasSizeGreaterThanOrEqualTo(5)
        routes.forEach { (method, pattern) ->
            val path = pattern.replace(Regex("\\{[^}]+}"), sub.toString())
            val token = if (pattern.startsWith("/internal/")) service else admin
            val response = api.send(method, path, body = """{"name":"Ana","email":"ana@example.test","status":"ACTIVE"}""", token = token)
            assertThat(response.status).describedAs("$method $pattern").isNotIn(401, 403)
        }
    }

    private fun isOurs(handler: HandlerMethod) = handler.beanType.name.startsWith("io.github.capozziv.digitalbanking.customer.")
}
