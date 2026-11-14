package io.github.capozziv.digitalbanking.statement.bootstrap.cache

import io.github.capozziv.digitalbanking.statement.application.port.StatementRepository
import io.github.capozziv.digitalbanking.statement.bootstrap.StatementIntegrationTest
import io.github.capozziv.digitalbanking.statement.bootstrap.web.ApiClient
import io.github.capozziv.digitalbanking.statement.bootstrap.web.ApiResponse
import io.github.capozziv.digitalbanking.statement.bootstrap.web.assertProblem
import io.github.capozziv.digitalbanking.statement.domain.EntryKind
import io.github.capozziv.digitalbanking.statement.domain.StatementEntry
import io.github.capozziv.digitalbanking.testing.redis.RedisTestSupport
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import io.micrometer.core.instrument.MeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.data.redis.core.StringRedisTemplate
import tools.jackson.databind.JsonNode
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.system.measureTimeMillis

/**
 * The first-page cache wired into the running service: web API, DynamoDB, Redis and the accounts queue. Unlike the
 * other web tests, the queue listeners stay on, since one test feeds an entry through the queue.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StatementCacheIntegrationTest : StatementIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var repository: StatementRepository

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    private val api by lazy { ApiClient(port) }

    @Test
    fun `the second read of the first page is served from the cache`() {
        val account = newAccount()
        repository.insertEntry(entry(account, 1))
        val hits = counter("statement.cache.requests", "hit")

        val first = api.get("/accounts/$account/statement", bearer = tokenOf(account))
        // Straight into DynamoDB, with no queue and no eviction: only the cache can hide it.
        repository.insertEntry(entry(account, 2))
        val second = api.get("/accounts/$account/statement", bearer = tokenOf(account))

        assertThat(sequences(first.body!!)).containsExactly(1L)
        assertThat(second.status).describedAs(second.rawBody).isEqualTo(200)
        assertThat(sequences(second.body!!)).containsExactly(1L)
        assertThat(counter("statement.cache.requests", "hit")).isEqualTo(hits + 1)
    }

    @Test
    fun `an entry consumed from the queue evicts the page and the next read shows it`() {
        val account = newAccount()
        repository.insertEntry(entry(account, 1))
        assertThat(sequences(api.get("/accounts/$account/statement", bearer = tokenOf(account)).body!!)).containsExactly(1L)
        assertThat(redisTemplate.hasKey(key(account))).isTrue()

        send(account, ledgerEntryRecorded(account, sequence = 2))

        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(100)).untilAsserted {
            assertThat(sequences(api.get("/accounts/$account/statement", bearer = tokenOf(account)).body!!)).containsExactly(2L, 1L)
        }
    }

    @Test
    fun `with redis paused the statement answers 200 from dynamodb in under 1 second`() {
        val account = newAccount()
        repository.insertEntry(entry(account, 1))
        repository.insertEntry(entry(account, 2))
        // Warm the Redis connection and the HTTP stack, so the paused read measures the command timeout.
        api.get("/accounts/${UUID.randomUUID()}/statement", bearer = admin)
        val errors = counter("statement.cache.requests", "error")

        var response: ApiResponse? = null
        RedisTestSupport.pause(redis)
        val elapsed =
            try {
                measureTimeMillis { response = api.get("/accounts/$account/statement", bearer = tokenOf(account)) }
            } finally {
                RedisTestSupport.unpause(redis)
            }

        assertThat(elapsed).isLessThan(1_000)
        assertThat(response!!.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(sequences(response.body!!)).containsExactly(2L, 1L)
        assertThat(counter("statement.cache.requests", "error")).isEqualTo(errors + 1)
    }

    @Test
    fun `a page in the cache is not served to a customer who is not the owner`() {
        val account = newAccount()
        repository.insertEntry(entry(account, 1))
        assertThat(api.get("/accounts/$account/statement", bearer = tokenOf(account)).status).isEqualTo(200)
        assertThat(redisTemplate.hasKey(key(account))).isTrue()

        val stranger = api.get("/accounts/$account/statement", bearer = JwtTestSupport.customerToken(UUID.randomUUID()))
        val unknown = api.get("/accounts/${UUID.randomUUID()}/statement", bearer = JwtTestSupport.customerToken(UUID.randomUUID()))
        val asAdmin = api.get("/accounts/$account/statement", bearer = admin)

        assertProblem(stranger, 404, "account-not-found")
        // The same body shape as an account that does not exist, apart from the id.
        assertProblem(unknown, 404, "account-not-found")
        assertThat(stranger.rawBody).doesNotContain("entryId")
        assertThat(asAdmin.status).describedAs(asAdmin.rawBody).isEqualTo(200)
        assertThat(sequences(asAdmin.body!!)).containsExactly(1L)
    }

    @Test
    fun `an unknown account answers 404 twice and stores nothing`() {
        val account = UUID.randomUUID()

        assertProblem(api.get("/accounts/$account/statement", bearer = admin), 404, "account-not-found")
        assertProblem(api.get("/accounts/$account/statement", bearer = admin), 404, "account-not-found")

        assertThat(redisTemplate.hasKey(key(account))).isFalse()
    }

    private val owners = mutableMapOf<UUID, UUID>()
    private val admin = JwtTestSupport.adminToken(UUID.randomUUID())

    /** An account registered as `AccountOpened` would, owned by a new customer. */
    private fun newAccount(): UUID {
        val account = UUID.randomUUID()
        owners[account] = UUID.randomUUID()
        repository.insertAccount(account, owners.getValue(account))
        return account
    }

    private fun tokenOf(account: UUID) = JwtTestSupport.customerToken(owners.getValue(account))

    private fun key(account: UUID) = "statement:first-page:v1:$account"

    private fun counter(
        name: String,
        result: String,
    ): Double =
        meterRegistry
            .find(name)
            .tag("result", result)
            .counter()
            ?.count() ?: 0.0

    private fun sequences(body: JsonNode): List<Long> {
        val entries = body.get("entries")
        return (0 until entries.size()).map { entries.get(it).get("sequence").asLong() }
    }

    private fun entry(
        account: UUID,
        sequence: Long,
    ) = StatementEntry(
        entryId = UUID.randomUUID(),
        accountId = account,
        sequence = sequence,
        kind = EntryKind.CREDIT,
        amountCents = 1_000,
        balanceAfterCents = 1_000 * sequence,
        description = "entry $sequence",
        createdAt = Instant.parse("2026-11-12T12:00:00Z").plusSeconds(sequence),
    )

    private fun send(
        account: UUID,
        body: String,
    ) {
        val queueUrl = sqs.getQueueUrl { it.queueName(ACCOUNTS_QUEUE) }.queueUrl()
        sqs.sendMessage {
            it
                .queueUrl(queueUrl)
                .messageGroupId(account.toString())
                .messageDeduplicationId(UUID.randomUUID().toString())
                .messageBody(body)
        }
    }

    private fun ledgerEntryRecorded(
        account: UUID,
        sequence: Long,
    ): String {
        val entryId = UUID.randomUUID()
        val payload =
            """{"entryId":"$entryId","accountId":"$account","sequence":$sequence,"kind":"CREDIT","amount":1000,""" +
                """"balanceAfter":${1000 * sequence},"description":"entry $sequence","idempotencyKey":"key-$entryId",""" +
                """"createdAt":"${Instant.now()}"}"""
        return """{"eventId":"${UUID.randomUUID()}","eventType":"LedgerEntryRecorded","eventVersion":1,""" +
            """"occurredAt":"${Instant.now()}","aggregateId":"agg","correlationId":"cache-it","payload":$payload}"""
    }
}
