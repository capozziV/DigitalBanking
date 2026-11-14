package io.github.capozziv.digitalbanking.customer.bootstrap

import io.github.capozziv.digitalbanking.customer.bootstrap.web.ApiClient
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import tools.jackson.databind.json.JsonMapper
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Events reach the outbox in the transaction of the change, with the correlation id of the request. */
class CustomerEventsIntegrationTest : CustomerIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    private val api by lazy { ApiClient(port) }
    private val mapper = JsonMapper.builder().build()
    private val body = """{"name":"Ana Souza","email":"ana@example.test"}"""
    private val admin = JwtTestSupport.adminToken(UUID.randomUUID())

    private data class OutboxRow(
        val eventType: String,
        val eventVersion: Int,
        val payload: String,
        val correlationId: String?,
    )

    private fun outboxOf(customerId: UUID): List<OutboxRow> =
        ownerJdbc
            .sql("select event_type, event_version, payload::text, correlation_id from outbox where aggregate_id = :id order by position")
            .param("id", customerId.toString())
            .query { rs, _ -> OutboxRow(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getString(4)) }
            .list()

    @Test
    fun `registering writes CustomerRegistered v1 with the correlation id of the request`() {
        val sub = UUID.randomUUID()

        api.post("/customers", body, JwtTestSupport.customerToken(sub), mapOf("X-Correlation-Id" to "corr-register"))

        val rows = outboxOf(sub)
        assertThat(rows).hasSize(1)
        assertThat(rows[0].eventType).isEqualTo("CustomerRegistered")
        assertThat(rows[0].eventVersion).isEqualTo(1)
        assertThat(rows[0].correlationId).isEqualTo("corr-register")
        val payload = mapper.readTree(rows[0].payload)
        assertThat(payload.get("customerId").asString()).isEqualTo(sub.toString())
        assertThat(payload.get("name").asString()).isEqualTo("Ana Souza")
        assertThat(payload.get("email").asString()).isEqualTo("ana@example.test")
    }

    @Test
    fun `a repeated registration writes no second event`() {
        val sub = UUID.randomUUID()
        val token = JwtTestSupport.customerToken(sub)

        api.post("/customers", body, token)
        api.post("/customers", body, token)

        assertThat(outboxOf(sub).map { it.eventType }).containsExactly("CustomerRegistered")
    }

    @Test
    fun `blocking and unblocking write one event each and the same status again writes none`() {
        val sub = UUID.randomUUID()
        api.post("/customers", body, JwtTestSupport.customerToken(sub))

        api.put("/admin/customers/$sub/status", """{"status":"BLOCKED"}""", admin, mapOf("X-Correlation-Id" to "corr-block"))
        api.put("/admin/customers/$sub/status", """{"status":"BLOCKED"}""", admin)
        api.put("/admin/customers/$sub/status", """{"status":"ACTIVE"}""", admin)
        api.put("/admin/customers/$sub/status", """{"status":"ACTIVE"}""", admin)

        val rows = outboxOf(sub)
        assertThat(rows.map { it.eventType }).containsExactly("CustomerRegistered", "CustomerBlocked", "CustomerUnblocked")
        assertThat(rows[1].correlationId).isEqualTo("corr-block")
        assertThat(mapper.readTree(rows[1].payload).get("customerId").asString()).isEqualTo(sub.toString())
        assertThat(rows.drop(1).map { it.eventVersion }).containsOnly(1)
    }

    @Test
    fun `a refused request writes nothing`() {
        val sub = UUID.randomUUID()

        api.post("/customers", """{"name":"","email":"x"}""", JwtTestSupport.customerToken(sub))
        api.put("/admin/customers/$sub/status", """{"status":"BLOCKED"}""", admin)

        assertThat(outboxOf(sub)).isEmpty()
    }

    @Test
    fun `parallel registrations of the same user create one customer, one 201 and one event`() {
        val sub = UUID.randomUUID()
        val token = JwtTestSupport.customerToken(sub)
        val parallel = 12

        val statuses =
            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                (1..parallel)
                    .map {
                        executor.submit<Int> {
                            api
                                .post(
                                    "/customers",
                                    body,
                                    token,
                                ).status
                        }
                    }.map { it.get(30, TimeUnit.SECONDS) }
            }

        assertThat(statuses.count { it == 201 }).describedAs("statuses: $statuses").isEqualTo(1)
        assertThat(statuses.count { it == 200 }).describedAs("statuses: $statuses").isEqualTo(parallel - 1)
        assertThat(outboxOf(sub).map { it.eventType }).containsExactly("CustomerRegistered")
        assertThat(
            ownerJdbc
                .sql("select count(*) from customer where id = :id")
                .param("id", sub)
                .query(Long::class.java)
                .single(),
        ).isEqualTo(1L)
    }

    @Test
    fun `parallel status changes leave one coherent history`() {
        val sub = UUID.randomUUID()
        api.post("/customers", body, JwtTestSupport.customerToken(sub))
        val parallel = 10

        val statuses =
            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                (1..parallel)
                    .map { n ->
                        val status = if (n % 2 == 0) "ACTIVE" else "BLOCKED"
                        executor.submit<Int> { api.put("/admin/customers/$sub/status", """{"status":"$status"}""", admin).status }
                    }.map { it.get(30, TimeUnit.SECONDS) }
            }

        assertThat(statuses).containsOnly(200)
        // Each event is a real transition: the history alternates, and the last event matches the stored status.
        val transitions = outboxOf(sub).drop(1).map { it.eventType }
        assertThat(transitions).isNotEmpty()
        transitions.zipWithNext().forEach { (a, b) -> assertThat(a).describedAs(transitions.toString()).isNotEqualTo(b) }
        assertThat(transitions.first()).isEqualTo("CustomerBlocked")
        val stored =
            ownerJdbc
                .sql("select status from customer where id = :id")
                .param("id", sub)
                .query(String::class.java)
                .single()
        assertThat(stored).isEqualTo(if (transitions.last() == "CustomerBlocked") "BLOCKED" else "ACTIVE")
    }
}
