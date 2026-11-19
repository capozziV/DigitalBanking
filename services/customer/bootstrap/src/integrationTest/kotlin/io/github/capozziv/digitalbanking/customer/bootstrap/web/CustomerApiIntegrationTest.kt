package io.github.capozziv.digitalbanking.customer.bootstrap.web

import io.github.capozziv.digitalbanking.customer.bootstrap.CustomerIntegrationTest
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import java.util.UUID

class CustomerApiIntegrationTest : CustomerIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }

    private fun encodedLookup(
        phone: String,
        token: String,
    ): java.net.http.HttpResponse<String> =
        java.net.http.HttpClient
            .newHttpClient()
            .send(
                java.net.http.HttpRequest
                    .newBuilder(java.net.URI.create("http://localhost:$port/customers/lookup?phone=${phone.replace("+", "%2B")}"))
                    .header("Authorization", "Bearer $token")
                    .build(),
                java.net.http.HttpResponse.BodyHandlers
                    .ofString(),
            )

    private fun body(
        name: String = "Ana Souza",
        email: String = "ana@example.test",
        phone: String = nextPhone(),
    ) = """{"name":"$name","email":"$email","phone":"$phone"}"""

    @Test
    fun `a customer registers with 201 and a repeat gives 200 with the same record`() {
        val sub = UUID.randomUUID()
        val token = JwtTestSupport.customerToken(sub)

        val first = api.post("/customers", body(), token)
        val repeat = api.post("/customers", body(name = "Other Name", email = "other@example.test"), token)

        assertThat(first.status).describedAs(first.rawBody).isEqualTo(201)
        assertThat(first.header("Location")).isEqualTo("/customers/me")
        assertThat(first.body!!.get("id").asString()).isEqualTo(sub.toString())
        assertThat(first.body.get("name").asString()).isEqualTo("Ana Souza")
        assertThat(first.body.get("email").asString()).isEqualTo("ana@example.test")
        assertThat(first.body.get("phone").asString()).startsWith("+55")
        assertThat(first.body.get("status").asString()).isEqualTo("ACTIVE")
        assertThat(repeat.status).describedAs(repeat.rawBody).isEqualTo(200)
        assertThat(repeat.body).isEqualTo(first.body)
    }

    @Test
    fun `GET me returns the registered customer`() {
        val sub = UUID.randomUUID()
        val token = JwtTestSupport.customerToken(sub)
        api.post("/customers", body(), token)

        val response = api.get("/customers/me", token)

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("id").asString()).isEqualTo(sub.toString())
        assertThat(response.body.get("status").asString()).isEqualTo("ACTIVE")
    }

    @Test
    fun `GET me without a registration is 404 customer-not-found`() {
        assertProblem(api.get("/customers/me", JwtTestSupport.customerToken(UUID.randomUUID())), 404, "customer-not-found")
    }

    @Test
    fun `an admin is also a customer of its own userId`() {
        val sub = UUID.randomUUID()
        val token = JwtTestSupport.adminToken(sub)

        val created = api.post("/customers", body(), token)
        val me = api.get("/customers/me", token)

        assertThat(created.status).describedAs(created.rawBody).isEqualTo(201)
        assertThat(created.body!!.get("id").asString()).isEqualTo(sub.toString())
        assertThat(me.status).isEqualTo(200)
    }

    @Test
    fun `without a token every route is 401 unauthorized`() {
        val id = UUID.randomUUID()

        assertProblem(api.post("/customers", body()), 401, "unauthorized")
        assertProblem(api.get("/customers/me"), 401, "unauthorized")
        assertProblem(api.put("/customers/me/phone", """{"phone":"+5511987654321"}"""), 401, "unauthorized")
        assertProblem(api.get("/admin/customers/$id"), 401, "unauthorized")
        assertProblem(api.put("/admin/customers/$id/status", """{"status":"BLOCKED"}"""), 401, "unauthorized")
        assertProblem(api.get("/internal/customers/$id"), 401, "unauthorized")
    }

    @Test
    fun `a service token cannot register or read me`() {
        val token = JwtTestSupport.serviceToken()

        assertProblem(api.post("/customers", body(), token), 403, "forbidden")
        assertProblem(api.get("/customers/me", token), 403, "forbidden")
    }

    @Test
    fun `an invalid body is 400 validation-error listing the fields`() {
        val token = JwtTestSupport.customerToken(UUID.randomUUID())

        val blankName = api.post("/customers", body(name = "   "), token)
        val badEmail = api.post("/customers", body(email = "not-an-email"), token)
        val empty = api.post("/customers", "{}", token)
        val longName = api.post("/customers", body(name = "a".repeat(121)), token)

        assertProblem(blankName, 400, "validation-error")
        assertThat(blankName.errorFields()).containsExactly("name")
        assertProblem(badEmail, 400, "validation-error")
        assertThat(badEmail.errorFields()).containsExactly("email")
        assertProblem(empty, 400, "validation-error")
        assertThat(empty.errorFields()).containsExactlyInAnyOrder("name", "email", "phone")
        assertProblem(longName, 400, "validation-error")
        assertThat(longName.errorFields()).containsExactly("name")
    }

    @Test
    fun `a phone that is not a Brazilian mobile number in E164 is 400 validation-error on the phone field`() {
        val token = JwtTestSupport.customerToken(UUID.randomUUID())

        listOf("11987654321", "(11) 98765-4321", "+551134567890", "+5501987654321", "+55119876543210", "").forEach {
            val response = api.post("/customers", body(phone = it), token)
            assertProblem(response, 400, "validation-error")
            assertThat(response.errorFields()).describedAs(it).containsOnly("phone")
        }
    }

    @Test
    fun `registering with a phone another customer has is 409 phone-already-in-use and creates nothing`() {
        val phone = nextPhone()
        api.post("/customers", body(phone = phone), JwtTestSupport.customerToken(UUID.randomUUID()))
        val second = UUID.randomUUID()
        val token = JwtTestSupport.customerToken(second)

        assertProblem(api.post("/customers", body(phone = phone), token), 409, "phone-already-in-use")

        assertProblem(api.get("/customers/me", token), 404, "customer-not-found")
    }

    @Test
    fun `the customer changes its phone and every read shows the new one`() {
        val sub = UUID.randomUUID()
        val token = JwtTestSupport.customerToken(sub)
        api.post("/customers", body(), token)
        val newPhone = nextPhone()

        val changed = api.put("/customers/me/phone", """{"phone":"$newPhone"}""", token)

        assertThat(changed.status).describedAs(changed.rawBody).isEqualTo(200)
        assertThat(changed.body!!.get("phone").asString()).isEqualTo(newPhone)
        assertThat(
            api
                .get("/customers/me", token)
                .body!!
                .get("phone")
                .asString(),
        ).isEqualTo(newPhone)
        assertThat(api.put("/customers/me/phone", """{"phone":"$newPhone"}""", token).status).isEqualTo(200)
    }

    @Test
    fun `change phone to a number in use is refused with 409 and the phone stays`() {
        val sub = UUID.randomUUID()
        val token = JwtTestSupport.customerToken(sub)
        val mine = nextPhone()
        val taken = nextPhone()
        api.post("/customers", body(phone = mine), token)
        api.post("/customers", body(phone = taken), JwtTestSupport.customerToken(UUID.randomUUID()))

        assertProblem(api.put("/customers/me/phone", """{"phone":"$taken"}""", token), 409, "phone-already-in-use")

        assertThat(
            api
                .get("/customers/me", token)
                .body!!
                .get("phone")
                .asString(),
        ).isEqualTo(mine)
    }

    @Test
    fun `change phone validates the number, needs a registration and refuses a service token`() {
        val token = JwtTestSupport.customerToken(UUID.randomUUID())

        val invalid = api.put("/customers/me/phone", """{"phone":"123"}""", token)
        val missing = api.put("/customers/me/phone", "{}", token)

        assertProblem(invalid, 400, "validation-error")
        assertThat(invalid.errorFields()).containsExactly("phone")
        assertProblem(missing, 400, "validation-error")
        assertProblem(api.put("/customers/me/phone", """{"phone":"${nextPhone()}"}""", token), 404, "customer-not-found")
        assertProblem(api.put("/customers/me/phone", """{"phone":"${nextPhone()}"}""", JwtTestSupport.serviceToken()), 403, "forbidden")
        assertProblem(api.put("/customers/me/phone", """{"phone":"${nextPhone()}"}"""), 401, "unauthorized")
    }

    @Test
    fun `lookup returns the customer id and the full name, and nothing else`() {
        val sub = UUID.randomUUID()
        val phone = nextPhone()
        api.post("/customers", body(name = "Ana Souza", phone = phone), JwtTestSupport.customerToken(sub))

        // The ApiClient re-encodes its path as a template, so this request goes by plain HTTP with the plus as %2B.
        val response = encodedLookup(phone, JwtTestSupport.customerToken(UUID.randomUUID()))

        assertThat(response.statusCode()).describedAs(response.body()).isEqualTo(200)
        val body =
            tools.jackson.databind.json.JsonMapper
                .builder()
                .build()
                .readTree(response.body())
        assertThat(body.propertyNames()).containsExactlyInAnyOrder("customerId", "fullName")
        assertThat(body.get("customerId").asString()).isEqualTo(sub.toString())
        assertThat(body.get("fullName").asString()).isEqualTo("Ana Souza")
    }

    @Test
    fun `lookup reads a plus sign that was not encoded in the query string`() {
        val sub = UUID.randomUUID()
        val phone = nextPhone()
        api.post("/customers", body(phone = phone), JwtTestSupport.customerToken(sub))

        val response = api.get("/customers/lookup?phone=$phone", JwtTestSupport.customerToken(UUID.randomUUID()))

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("customerId").asString()).isEqualTo(sub.toString())
    }

    @Test
    fun `lookup of an unknown phone is 404 and of an invalid or missing phone is 400`() {
        val token = JwtTestSupport.customerToken(UUID.randomUUID())

        assertProblem(api.get("/customers/lookup?phone=${nextPhone()}", token), 404, "customer-not-found")
        val invalid = api.get("/customers/lookup?phone=11987654321", token)
        val missing = api.get("/customers/lookup", token)
        assertProblem(invalid, 400, "validation-error")
        assertThat(invalid.errorFields()).containsExactly("phone")
        assertProblem(missing, 400, "validation-error")
        assertThat(missing.errorFields()).containsExactly("phone")
    }

    @Test
    fun `lookup requires authentication and refuses a service token`() {
        assertProblem(api.get("/customers/lookup?phone=+5511987654321"), 401, "unauthorized")
        assertProblem(api.get("/customers/lookup?phone=+5511987654321", JwtTestSupport.serviceToken()), 403, "forbidden")
    }

    @Test
    fun `the admin reads a customer and the customer token gets 403`() {
        val sub = UUID.randomUUID()
        api.post("/customers", body(), JwtTestSupport.customerToken(sub))

        val asAdmin = api.get("/admin/customers/$sub", JwtTestSupport.adminToken(UUID.randomUUID()))
        val asCustomer = api.get("/admin/customers/$sub", JwtTestSupport.customerToken(sub))

        assertThat(asAdmin.status).describedAs(asAdmin.rawBody).isEqualTo(200)
        assertThat(asAdmin.body!!.get("id").asString()).isEqualTo(sub.toString())
        assertThat(asAdmin.body.get("email").asString()).isEqualTo("ana@example.test")
        assertProblem(asCustomer, 403, "forbidden")
    }

    @Test
    fun `admin routes answer 404 for an unknown customer and 400 for an id that is not a UUID`() {
        val admin = JwtTestSupport.adminToken(UUID.randomUUID())

        assertProblem(api.get("/admin/customers/${UUID.randomUUID()}", admin), 404, "customer-not-found")
        assertProblem(api.put("/admin/customers/${UUID.randomUUID()}/status", """{"status":"BLOCKED"}""", admin), 404, "customer-not-found")
        assertProblem(api.get("/admin/customers/abc", admin), 400, "validation-error")
    }

    @Test
    fun `the admin blocks and unblocks a customer and the status shows on every read`() {
        val sub = UUID.randomUUID()
        val customerToken = JwtTestSupport.customerToken(sub)
        val admin = JwtTestSupport.adminToken(UUID.randomUUID())
        api.post("/customers", body(), customerToken)

        val blocked = api.put("/admin/customers/$sub/status", """{"status":"BLOCKED"}""", admin)

        assertThat(blocked.status).describedAs(blocked.rawBody).isEqualTo(200)
        assertThat(blocked.body!!.get("status").asString()).isEqualTo("BLOCKED")
        assertThat(
            api
                .get("/customers/me", customerToken)
                .body!!
                .get("status")
                .asString(),
        ).isEqualTo("BLOCKED")
        assertThat(
            api
                .get("/internal/customers/$sub", JwtTestSupport.serviceToken())
                .body!!
                .get("status")
                .asString(),
        ).isEqualTo("BLOCKED")

        val unblocked = api.put("/admin/customers/$sub/status", """{"status":"ACTIVE"}""", admin)

        assertThat(unblocked.body!!.get("status").asString()).isEqualTo("ACTIVE")
        // Blocking a blocked customer again is not an error.
        api.put("/admin/customers/$sub/status", """{"status":"BLOCKED"}""", admin)
        assertThat(api.put("/admin/customers/$sub/status", """{"status":"BLOCKED"}""", admin).status).isEqualTo(200)
    }

    @Test
    fun `an invalid status is 400 and the customer token cannot change it`() {
        val sub = UUID.randomUUID()
        val customerToken = JwtTestSupport.customerToken(sub)
        api.post("/customers", body(), customerToken)

        val unknown = api.put("/admin/customers/$sub/status", """{"status":"FROZEN"}""", JwtTestSupport.adminToken(UUID.randomUUID()))
        val missing = api.put("/admin/customers/$sub/status", "{}", JwtTestSupport.adminToken(UUID.randomUUID()))
        val forbidden = api.put("/admin/customers/$sub/status", """{"status":"BLOCKED"}""", customerToken)

        assertProblem(unknown, 400, "validation-error")
        assertThat(unknown.errorFields()).containsExactly("status")
        assertProblem(missing, 400, "validation-error")
        assertProblem(forbidden, 403, "forbidden")
        assertThat(
            api
                .get("/customers/me", customerToken)
                .body!!
                .get("status")
                .asString(),
        ).isEqualTo("ACTIVE")
    }

    @Test
    fun `the internal lookup gives only id and status to a service token`() {
        val sub = UUID.randomUUID()
        api.post("/customers", body(), JwtTestSupport.customerToken(sub))

        val response = api.get("/internal/customers/$sub", JwtTestSupport.serviceToken())

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.propertyNames()).containsExactlyInAnyOrder("id", "status")
        assertThat(response.body.get("id").asString()).isEqualTo(sub.toString())
        assertThat(response.body.get("status").asString()).isEqualTo("ACTIVE")
    }

    @Test
    fun `the internal lookup of an unknown customer is 404`() {
        assertProblem(api.get("/internal/customers/${UUID.randomUUID()}", JwtTestSupport.serviceToken()), 404, "customer-not-found")
    }

    @Test
    fun `the internal lookup is 403 for a customer token, even for its own id, and for an admin token`() {
        val sub = UUID.randomUUID()
        api.post("/customers", body(), JwtTestSupport.customerToken(sub))

        assertProblem(api.get("/internal/customers/$sub", JwtTestSupport.customerToken(sub)), 403, "forbidden")
        assertProblem(api.get("/internal/customers/$sub", JwtTestSupport.adminToken(UUID.randomUUID())), 403, "forbidden")
    }

    @Test
    fun `expired tokens and tokens from another issuer are 401`() {
        val sub = UUID.randomUUID().toString()
        val expired = JwtTestSupport.token(sub, expiresIn = java.time.Duration.ofMinutes(-5))
        val otherIssuer = JwtTestSupport.token(sub, issuer = "http://other.test")

        assertProblem(api.get("/customers/me", expired), 401, "unauthorized")
        assertProblem(api.get("/customers/me", otherIssuer), 401, "unauthorized")
    }

    @Test
    fun `health is open without details and metrics are for the admin only`() {
        val health = api.get("/actuator/health")

        assertThat(health.status).isEqualTo(200)
        assertThat(health.body!!.get("status").asString()).isEqualTo("UP")
        assertThat(health.body.get("components")).isNull()
        assertProblem(api.get("/actuator/metrics", JwtTestSupport.customerToken(UUID.randomUUID())), 403, "forbidden")
        assertThat(api.get("/actuator/metrics", JwtTestSupport.adminToken(UUID.randomUUID())).status).isEqualTo(200)
    }
}
