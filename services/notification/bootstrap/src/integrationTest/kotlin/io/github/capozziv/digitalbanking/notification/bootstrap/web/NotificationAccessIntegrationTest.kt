package io.github.capozziv.digitalbanking.notification.bootstrap.web

import io.github.capozziv.digitalbanking.notification.application.port.NotificationRepository
import io.github.capozziv.digitalbanking.notification.bootstrap.persistence.aNotification
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import java.time.Duration
import java.util.UUID

/** JWT on the notification API: 401, customers see only their own, an admin may read a customer's list. */
class NotificationAccessIntegrationTest : NotificationWebIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var repository: NotificationRepository

    private val api by lazy { ApiClient(port) }

    private val customer = UUID.randomUUID()
    private val other = UUID.randomUUID()

    @BeforeEach
    fun seed() {
        ownerJdbc.sql("delete from notifications").update()
        repository.saveAll(
            listOf(aNotification(customerId = customer), aNotification(customerId = other), aNotification(customerId = other)),
        )
    }

    private fun count(response: ApiResponse) = response.body!!.get("items").size()

    @Test
    fun `without a token both routes are 401 unauthorized`() {
        assertProblem(api.get("/notifications"), 401, "unauthorized")
        assertProblem(api.post("/notifications/read", """{"upTo":"${UUID.randomUUID()}"}"""), 401, "unauthorized")
    }

    @Test
    fun `an expired token, one from another issuer and a malformed one are 401`() {
        val expired = JwtTestSupport.token(customer.toString(), expiresIn = Duration.ofMinutes(-5))
        val otherIssuer = JwtTestSupport.token(customer.toString(), issuer = "http://other.test")

        listOf(expired, otherIssuer, "not-a-jwt").forEach { assertProblem(api.get("/notifications", bearer = it), 401, "unauthorized") }
    }

    @Test
    fun `a customer lists only their own notifications`() {
        assertThat(count(api.get("/notifications", bearer = JwtTestSupport.customerToken(customer)))).isEqualTo(1)
        assertThat(count(api.get("/notifications", bearer = JwtTestSupport.customerToken(other)))).isEqualTo(2)
    }

    @Test
    fun `a customer who names their own id is served, one who names another customer is 404`() {
        val token = JwtTestSupport.customerToken(customer)

        assertThat(count(api.get("/notifications?customerId=$customer", bearer = token))).isEqualTo(1)
        val foreign = api.get("/notifications?customerId=$other", bearer = token)
        assertProblem(foreign, 404, "notification-not-found")
        assertThat(foreign.rawBody).doesNotContain(other.toString())
    }

    @Test
    fun `an admin names a customer to read their notifications, and reads their own without the parameter`() {
        val admin = JwtTestSupport.adminToken(UUID.randomUUID())

        assertThat(count(api.get("/notifications?customerId=$other", bearer = admin))).isEqualTo(2)
        assertThat(count(api.get("/notifications?customerId=$customer", bearer = admin))).isEqualTo(1)
        assertThat(count(api.get("/notifications", bearer = admin))).isZero()
    }

    @Test
    fun `marking as read touches only the caller's notifications, even for an admin or with a customerId in the body`() {
        val theirs = repository.page(other, null, 10).first()
        val admin = JwtTestSupport.adminToken(UUID.randomUUID())

        // An admin can neither mark nor even anchor on a notification of a customer.
        assertProblem(api.post("/notifications/read", """{"upTo":"${theirs.id}"}""", bearer = admin), 404, "notification-not-found")
        // A customerId in the body is not part of the contract: it is ignored, the caller is the token.
        assertProblem(
            api.post(
                "/notifications/read",
                """{"customerId":"$other","upTo":"${theirs.id}"}""",
                bearer = JwtTestSupport.customerToken(customer),
            ),
            404,
            "notification-not-found",
        )
        assertThat(repository.find(theirs.id)!!.readAt).isNull()
    }

    @Test
    fun `an admin marks as read their own notifications`() {
        val adminId = UUID.randomUUID()
        val own = aNotification(customerId = adminId)
        repository.saveAll(listOf(own))

        val response = api.post("/notifications/read", """{"upTo":"${own.id}"}""", bearer = JwtTestSupport.adminToken(adminId))

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(repository.find(own.id)!!.readAt).isNotNull()
    }

    @Test
    fun `a service token is 403 forbidden`() {
        assertThat(api.get("/notifications", bearer = JwtTestSupport.serviceToken()).status).isEqualTo(403)
    }

    @Test
    fun `the actuator is open for health only, metrics are for an admin`() {
        assertThat(api.get("/actuator/health").status).isEqualTo(200)
        assertProblem(api.get("/actuator/metrics"), 401, "unauthorized")
        assertProblem(api.get("/actuator/metrics", bearer = JwtTestSupport.customerToken(customer)), 403, "forbidden")
        assertThat(api.get("/actuator/metrics", bearer = JwtTestSupport.adminToken(customer)).status).isEqualTo(200)
    }

    @Test
    fun `a route nobody declared is denied`() {
        assertProblem(api.get("/notifications/everything", bearer = JwtTestSupport.adminToken(customer)), 403, "forbidden")
    }
}
