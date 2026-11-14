package io.github.capozziv.digitalbanking.accounts.bootstrap.web

import io.github.capozziv.digitalbanking.accounts.bootstrap.AccountsIntegrationTest
import io.github.capozziv.digitalbanking.accounts.bootstrap.CustomerServiceStub.Behavior
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import java.time.Duration
import java.util.UUID

/**
 * Opening an account asks the `customer` service (here the [io.github.capozziv.digitalbanking.accounts.bootstrap.CustomerServiceStub])
 * with a machine token; only an ACTIVE customer gets an account, and nothing is written otherwise.
 */
class AccountOpeningCustomerIntegrationTest : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }
    private val customerId = UUID.randomUUID()

    @Test
    fun `an active customer gets 201, and the customer service was asked with a service token and the correlation id`() {
        val correlationId = UUID.randomUUID().toString()

        val response =
            api.send(
                org.springframework.http.HttpMethod.POST,
                "/accounts",
                headers =
                    mapOf("Idempotency-Key" to UUID.randomUUID().toString(), "X-Correlation-Id" to correlationId) +
                        asCustomer(customerId),
            )

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(201)
        val lookup = customerService.lookupsOf(customerId).single()
        val token = lookup.authorization!!.removePrefix("Bearer ")
        val claims = JwtTestSupport.jwtDecoder().decode(token)
        assertThat(claims.getClaimAsString("scope")).isEqualTo(JwtTestSupport.SERVICE_SCOPE)
        assertThat(lookup.correlationId).isEqualTo(correlationId)
    }

    @Test
    fun `an unknown customer is 422 customer-not-found and nothing is written`() {
        customerService.answer(customerId, Behavior.NotFound)

        assertProblem(api.openAccount(customerId), 422, "customer-not-found")
        assertNothingWrittenFor(customerId)
    }

    @Test
    fun `a blocked customer is 422 customer-blocked and nothing is written`() {
        customerService.answer(customerId, Behavior.Blocked)

        assertProblem(api.openAccount(customerId), 422, "customer-blocked")
        assertNothingWrittenFor(customerId)
    }

    @Test
    fun `a customer service answering 500 is 503 customers-unavailable and nothing is written`() {
        customerService.answer(customerId, Behavior.ServerError)

        assertProblem(api.openAccount(customerId), 503, "customers-unavailable")
        assertNothingWrittenFor(customerId)
    }

    @Test
    fun `a customer service slower than the timeout is 503 customers-unavailable within the timeout, and nothing is written`() {
        customerService.answer(customerId, Behavior.Slow(Duration.ofSeconds(3)))
        api.openAccount(UUID.randomUUID()) // warms the cached service token, so the timing below is the customer call only

        val started = System.nanoTime()
        val response = api.openAccount(customerId)
        val elapsed = Duration.ofNanos(System.nanoTime() - started)

        assertProblem(response, 503, "customers-unavailable")
        assertThat(elapsed).isLessThan(Duration.ofSeconds(2))
        assertNothingWrittenFor(customerId)
    }

    @Test
    fun `a blocked customer cannot get an account even retrying an opening key that was accepted before`() {
        val openingKey = UUID.randomUUID().toString()
        assertThat(api.openAccount(customerId, openingKey).status).isEqualTo(201)
        customerService.answer(customerId, Behavior.Blocked)

        assertProblem(api.openAccount(customerId, openingKey), 422, "customer-blocked")
    }

    private fun assertNothingWrittenFor(customerId: UUID) {
        val accounts =
            ownerJdbc
                .sql("select count(*) from account where customer_id = :id")
                .param("id", customerId)
                .query(Long::class.java)
                .single()
        val events =
            ownerJdbc
                .sql("select count(*) from outbox where event_type = 'AccountOpened' and payload::text like :id")
                .param("id", "%$customerId%")
                .query(Long::class.java)
                .single()
        assertThat(accounts).isZero()
        assertThat(events).isZero()
    }
}
