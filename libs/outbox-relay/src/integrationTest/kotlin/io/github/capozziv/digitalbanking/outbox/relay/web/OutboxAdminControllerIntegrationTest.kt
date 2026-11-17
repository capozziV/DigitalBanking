package io.github.capozziv.digitalbanking.outbox.relay.web

import io.github.capozziv.digitalbanking.outbox.relay.OutboxStore
import io.github.capozziv.digitalbanking.outbox.relay.spring.OutboxRelayConfigurationSupport
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Clock
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * The admin endpoint that every service with a relay gets from [OutboxRelayConfigurationSupport]: behind the security
 * of the services (`/admin/` is for admins), on a real PostgreSQL, with the relay polling only at startup.
 */
@Testcontainers
@SpringBootTest(
    classes = [OutboxAdminControllerIntegrationTest.TestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "demo.aws.region=us-east-1",
        "demo.aws.topic-arn=arn:aws:sns:us-east-1:000000000000:demo.fifo",
        "demo.aws.endpoint=http://127.0.0.1:1",
        "demo.outbox.poll-interval=1h",
        // A discard waits 400 ms for the relay lock before answering 503.
        "demo.outbox.discard-lock-timeout=400ms",
        "demo.outbox.max-cycle-duration=100ms",
        "demo.outbox.publish-timeout=100ms",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:outbox/outbox-table.sql",
    ],
)
class OutboxAdminControllerIntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @Autowired
    private lateinit var store: OutboxStore

    private val http = HttpClient.newHttpClient()
    private val mapper = JsonMapper.builder().build()
    private val adminToken = JwtTestSupport.adminToken(ADMIN_ID)
    private val customerToken = JwtTestSupport.customerToken(UUID.randomUUID())

    @BeforeEach
    fun cleanOutbox() {
        jdbcClient.sql("truncate outbox").update()
    }

    @Test
    fun `an admin lists the stuck events, oldest first`() {
        val stuck = insert("acc-A", attempts = 12, lastError = "InvalidParameterException: message too long")
        insert("acc-A")
        insert("acc-healthy")

        val response = send("GET", "/admin/outbox/stuck-events", adminToken)

        assertThat(response.statusCode()).describedAs(response.body()).isEqualTo(200)
        val events = json(response)
        assertThat(events.size()).isEqualTo(1)
        val event = events.get(0)
        assertThat(event.get("eventId").asString()).isEqualTo(stuck.toString())
        assertThat(event.get("aggregateId").asString()).isEqualTo("acc-A")
        assertThat(event.get("eventType").asString()).isEqualTo("LedgerEntryRecorded")
        assertThat(event.get("attempts").asInt()).isEqualTo(12)
        assertThat(event.get("lastError").asString()).contains("message too long")
        assertThat(event.get("occurredAt").asString()).isNotBlank()
    }

    @Test
    fun `an admin discards a stuck event and the rest of its aggregate goes on, once and for all`() {
        val stuck = insert("acc-A", attempts = 12, lastError = "InvalidParameterException: message too long")
        val next = insert("acc-A")

        val discarded = send("POST", "/admin/outbox/events/$stuck/discard", adminToken, """{"reason":"payload over the SNS limit"}""")
        val again = send("POST", "/admin/outbox/events/$stuck/discard", adminToken, """{"reason":"retried by the admin"}""")

        assertThat(discarded.statusCode()).describedAs(discarded.body()).isEqualTo(204)
        assertThat(again.statusCode()).describedAs(again.body()).isEqualTo(204)
        assertThat(store.asLeader(4_242L) { store.fetchPending(10) }!!.map { it.eventId }).containsExactly(next)
        assertThat(lastError(stuck)).contains(ADMIN_ID.toString()).contains("payload over the SNS limit").contains("message too long")
        assertThat(json(send("GET", "/admin/outbox/stuck-events", adminToken)).size()).isZero()
    }

    @Test
    fun `discard answers 404 for an unknown event and 409 for one that does not hold its aggregate back`() {
        val neverFailed = insert("acc-A")
        val published = insert("acc-B", attempts = 3, publishedAt = true)

        assertProblem(discard(UUID.randomUUID().toString()), 404, "outbox-event-not-found")
        assertProblem(discard(neverFailed.toString()), 409, "outbox-event-not-stuck")
        assertProblem(discard(published.toString()), 409, "outbox-event-not-stuck")
    }

    @Test
    fun `discard answers 400 for an event id that is not a UUID`() {
        val problem = discard("not-a-uuid")

        assertProblem(problem, 400, "validation-error")
        assertThat(json(problem).get("title").asString()).isEqualTo("Validation error")
        assertThat(
            json(problem)
                .get("errors")
                .get(0)
                .get("field")
                .asString(),
        ).isEqualTo("eventId")
    }

    @Test
    fun `discard answers 503 while the relay cycle holds the lock past the wait, and changes nothing`() {
        val stuck = insert("acc-A", attempts = 12, lastError = "boom")
        val cycleInside = CountDownLatch(1)
        val endCycle = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val cycle =
                executor.submit<Boolean> {
                    store.asLeader(4_242L) {
                        cycleInside.countDown()
                        endCycle.await(10, TimeUnit.SECONDS)
                    }
                }
            assertThat(cycleInside.await(10, TimeUnit.SECONDS)).isTrue()

            val busy = discard(stuck.toString())

            assertProblem(busy, 503, "outbox-relay-busy")
            assertThat(busy.headers().firstValue("Retry-After")).isPresent()
            endCycle.countDown()
            cycle.get(10, TimeUnit.SECONDS)
        } finally {
            endCycle.countDown()
            executor.shutdownNow()
        }
        assertThat(store.stuckEvents(10).map { it.eventId }).containsExactly(stuck)
    }

    @Test
    fun `discard requires a reason`() {
        val stuck = insert("acc-A", attempts = 1, lastError = "boom")

        assertProblem(send("POST", "/admin/outbox/events/$stuck/discard", adminToken, """{"reason":"  "}"""), 400, "validation-error")
        assertProblem(send("POST", "/admin/outbox/events/$stuck/discard", adminToken, "{}"), 400, "validation-error")
        assertThat(store.stuckEvents(10).map { it.eventId }).containsExactly(stuck)
    }

    @Test
    fun `only an admin reaches the endpoint, 403 for a customer and 401 without a token`() {
        val stuck = insert("acc-A", attempts = 12, lastError = "boom")
        val body = """{"reason":"poison"}"""

        assertProblem(send("GET", "/admin/outbox/stuck-events", customerToken), 403, "forbidden")
        assertProblem(send("POST", "/admin/outbox/events/$stuck/discard", customerToken, body), 403, "forbidden")
        assertProblem(send("POST", "/admin/outbox/events/$stuck/discard", JwtTestSupport.serviceToken(), body), 403, "forbidden")
        assertProblem(send("POST", "/admin/outbox/events/$stuck/discard", null, body), 401, "unauthorized")
        assertThat(store.stuckEvents(10).map { it.eventId }).containsExactly(stuck)
    }

    @Test
    fun `the OpenAPI contract documents both operations and their errors`() {
        val paths = json(send("GET", "/v3/api-docs", null)).get("paths")

        val list = paths.get("/admin/outbox/stuck-events").get("get")
        val discard = paths.get("/admin/outbox/events/{eventId}/discard").get("post")
        assertThat(list.get("responses").has("200")).isTrue()
        assertThat(
            discard
                .get("responses")
                .get("404")
                .get("description")
                .asString(),
        ).contains("/problems/outbox-event-not-found")
        assertThat(
            discard
                .get("responses")
                .get("409")
                .get("description")
                .asString(),
        ).contains("/problems/outbox-event-not-stuck")
        assertThat(
            discard
                .get("responses")
                .get("400")
                .get("description")
                .asString(),
        ).contains("/problems/validation-error")
        assertThat(
            discard
                .get("responses")
                .get("503")
                .get("description")
                .asString(),
        ).contains("/problems/outbox-relay-busy")
        assertThat(discard.get("description").asString()).contains("AccountOpened")
        assertThat(discard.get("responses").has("204")).isTrue()
    }

    private fun discard(eventId: String) = send("POST", "/admin/outbox/events/$eventId/discard", adminToken, """{"reason":"poison"}""")

    private fun insert(
        aggregateId: String,
        attempts: Int = 0,
        lastError: String? = null,
        publishedAt: Boolean = false,
    ): UUID {
        val eventId = UUID.randomUUID()
        jdbcClient
            .sql(
                """
                insert into outbox (event_id, aggregate_id, event_type, event_version, payload, occurred_at, attempts, last_error, published_at)
                values (:eventId, :aggregateId, 'LedgerEntryRecorded', 1, '{}', now(), :attempts, :lastError,
                        case when :published then now() end)
                """.trimIndent(),
            ).param("eventId", eventId)
            .param("aggregateId", aggregateId)
            .param("attempts", attempts)
            .param("lastError", lastError)
            .param("published", publishedAt)
            .update()
        return eventId
    }

    private fun lastError(eventId: UUID): String =
        jdbcClient
            .sql("select last_error from outbox where event_id = :eventId")
            .param("eventId", eventId)
            .query(String::class.java)
            .single()

    private fun send(
        method: String,
        path: String,
        token: String?,
        body: String? = null,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder(URI.create("http://127.0.0.1:$port$path"))
                .method(method, body?.let { HttpRequest.BodyPublishers.ofString(it) } ?: HttpRequest.BodyPublishers.noBody())
                .apply { if (body != null) header("Content-Type", "application/json") }
                .apply { if (token != null) header("Authorization", "Bearer $token") }
                .build()
        return http.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun json(response: HttpResponse<String>): JsonNode = mapper.readTree(response.body())

    private fun assertProblem(
        response: HttpResponse<String>,
        status: Int,
        slug: String,
    ) {
        assertThat(response.statusCode()).describedAs(response.body()).isEqualTo(status)
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json")
        val problem = json(response)
        assertThat(problem.get("type").asString()).isEqualTo("/problems/$slug")
        assertThat(problem.get("status").asInt()).isEqualTo(status)
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(DemoRelayConfiguration::class, JwtTestSupport.DecoderConfiguration::class)
    class TestApplication {
        @Bean
        fun clock(): Clock = Clock.systemUTC()

        @Bean
        fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()
    }

    @Configuration(proxyBeanMethods = false)
    class DemoRelayConfiguration : OutboxRelayConfigurationSupport(propertyPrefix = "demo", lockKey = 4_242L)

    companion object {
        private val ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-00000000a0a0")

        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17-alpine")

        @JvmStatic
        @DynamicPropertySource
        fun datasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", { postgres.jdbcUrl })
            registry.add("spring.datasource.username", { postgres.username })
            registry.add("spring.datasource.password", { postgres.password })
        }
    }
}
