package io.github.capozziv.digitalbanking.accounts.bootstrap

import io.github.capozziv.digitalbanking.accounts.bootstrap.web.ApiClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** With the SNS endpoint unreachable, the relay records failed attempts and the service stays healthy. */
@TestPropertySource(
    properties = [
        "accounts.outbox.relay-enabled=true",
        "accounts.outbox.poll-interval=100ms",
        "accounts.outbox.publish-timeout=500ms",
        "accounts.aws.endpoint=http://127.0.0.1:1",
    ],
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OutboxRelayUnreachableBrokerIT : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var context: ConfigurableApplicationContext

    private val api by lazy { ApiClient(port) }

    /**
     * The scheduler of Spring Boot (bean `taskScheduler`, a SimpleAsyncTaskScheduler with virtual threads)
     * interrupts its running tasks when closed; the relay scheduler must be destroyed before it, so that its
     * own wait for the cycle in progress comes first.
     */
    @Test
    fun `the relay scheduler is destroyed before the task scheduler`() {
        assertThat(context.containsBean("taskScheduler")).isTrue()
        assertThat(context.beanFactory.getDependenciesForBean("outboxRelayScheduler")).contains("taskScheduler")
    }

    @Test
    fun `failed publications increase attempts and health stays UP`() {
        markPendingOutboxEventsPublished()
        val opened =
            api.post(
                "/accounts",
                """{"customerId":"${UUID.randomUUID()}"}""",
                mapOf("Idempotency-Key" to UUID.randomUUID().toString()),
            )
        assertThat(opened.status).isEqualTo(201)
        val account = opened.body!!.get("id").asString()

        val (attempts, lastError, publishedAt) = awaitAttempts(account, atLeast = 3)

        assertThat(attempts).isGreaterThanOrEqualTo(3)
        assertThat(lastError).isNotBlank()
        assertThat(publishedAt).isNull()
        val health = api.get("/actuator/health")
        assertThat(health.status).isEqualTo(200)
        assertThat(health.body!!.get("status").asString()).isEqualTo("UP")
        val failures = api.get("/actuator/metrics/outbox.publish.failures")
        assertThat(
            failures.body!!
                .get("measurements")
                .get(0)
                .get("value")
                .asDouble(),
        ).isGreaterThanOrEqualTo(3.0)
    }

    private fun awaitAttempts(
        account: String,
        atLeast: Int,
    ): Triple<Int, String?, Any?> {
        val deadline = Instant.now().plus(Duration.ofSeconds(30))
        while (true) {
            val row =
                ownerJdbc
                    .sql("select attempts, last_error, published_at from outbox where aggregate_id = :id")
                    .param("id", account)
                    .query()
                    .singleRow()
            val attempts = (row["attempts"] as Number).toInt()
            if (attempts >= atLeast) return Triple(attempts, row["last_error"] as String?, row["published_at"])
            check(Instant.now().isBefore(deadline)) {
                "outbox event of $account reached only $attempts attempts (expected $atLeast); " +
                    "last_error=${row["last_error"]}"
            }
            Thread.sleep(100)
        }
    }
}
