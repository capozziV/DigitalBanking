package io.github.capozziv.digitalbanking.e2e

import io.github.capozziv.digitalbanking.testing.localstack.LocalStackTestSupport
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.localstack.LocalStackContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.MountableFile
import software.amazon.awssdk.services.sqs.SqsClient
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

/**
 * Everything the end-to-end tests run against, as in the local environment: one Redis (the `statement` first-page cache
 * and the `transfers` daily limit), one PostgreSQL with the `accounts`, `transfers`, `notification` and `customer`
 * databases and their owner/app users (created by the compose init script itself), LocalStack with the topics,
 * inspection queues, notification queues and statement queue and table of the compose init scripts, the
 * `mock-oauth2-server` with the issuer config of the compose (every service validates its tokens, and `accounts` and
 * `transfers` get their machine tokens from it), the five services as `java -jar` processes, and [AccountsProxy]
 * between `transfers` and `accounts`. The API clients get their tokens from [tokens].
 *
 * The saga runs with short times (see [SAGA_ARGUMENTS]) so the deadline and lease scenarios take seconds, not minutes.
 */
class EndToEndEnvironment : AutoCloseable {
    private val postgres: PostgreSQLContainer =
        PostgreSQLContainer(POSTGRES_IMAGE)
            .withEnv(DATABASE_PASSWORDS)
            .apply {
                scripts(systemPath("e2e.postgres-init-dir")).forEach { script ->
                    withCopyFileToContainer(MountableFile.forHostPath(script, EXECUTABLE), "/docker-entrypoint-initdb.d/${script.fileName}")
                }
            }
    private val redis: GenericContainer<*> = GenericContainer<Nothing>(REDIS_IMAGE).withExposedPorts(REDIS_PORT)
    private val oauth: GenericContainer<*> =
        GenericContainer<Nothing>(MOCK_OAUTH2_IMAGE).apply {
            withExposedPorts(MOCK_OAUTH2_PORT)
            withEnv("JSON_CONFIG_PATH", MOCK_OAUTH2_CONFIG)
            withCopyFileToContainer(MountableFile.forHostPath(systemPath("e2e.oauth-config")), MOCK_OAUTH2_CONFIG)
            waitingFor(Wait.forHttp("/default/.well-known/openid-configuration").forStatusCode(200))
        }
    private lateinit var localStack: LocalStackContainer
    private lateinit var sqs: SqsClient

    lateinit var accounts: ServiceProcess
        private set
    lateinit var transfers: ServiceProcess
        private set
    lateinit var notification: ServiceProcess
        private set
    lateinit var statement: ServiceProcess
        private set
    lateinit var customer: ServiceProcess
        private set
    lateinit var tokens: TokenIssuer
        private set
    lateinit var customerApi: CustomerApiClient
        private set
    lateinit var proxy: AccountsProxy
        private set
    lateinit var accountsApi: AccountsApiClient
        private set
    lateinit var transfersApi: TransfersApiClient
        private set
    lateinit var notificationApi: NotificationApiClient
        private set
    lateinit var statementApi: StatementApiClient
        private set
    lateinit var notificationQueues: NotificationQueues
        private set
    lateinit var transfersDatabase: TransfersDatabase
        private set
    lateinit var transferEvents: TransferEventsQueue
        private set

    fun processes(): List<ServiceProcess> =
        listOfNotNull(if (::accounts.isInitialized) accounts else null) +
            listOfNotNull(if (::transfers.isInitialized) transfers else null) +
            listOfNotNull(if (::notification.isInitialized) notification else null) +
            listOfNotNull(if (::statement.isInitialized) statement else null) +
            listOfNotNull(if (::customer.isInitialized) customer else null)

    /** Starts LocalStack first: without the token the whole class is skipped locally (and fails on CI) right away. */
    fun start() {
        localStack = LocalStackTestSupport.container(listOf("sns", "sqs", "dynamodb")).withEnv(NOTIFICATION_QUEUE_SETTINGS)
        scripts(systemPath("e2e.localstack-init-dir")).forEach { script ->
            localStack.withCopyFileToContainer(
                MountableFile.forHostPath(script, EXECUTABLE),
                "/etc/localstack/init/ready.d/${script.fileName}",
            )
        }
        localStack.start()
        awaitLocalStackInit()
        postgres.start()
        redis.start()
        oauth.start()
        tokens = TokenIssuer(issuer())

        sqs = LocalStackTestSupport.sqsClient(localStack)
        transferEvents = TransferEventsQueue(sqs, sqs.getQueueUrl { it.queueName(TRANSFERS_INSPECTION_QUEUE) }.queueUrl())
        notificationQueues = NotificationQueues(sqs)
        transfersDatabase =
            TransfersDatabase(jdbcUrl("transfers"), "transfers_owner", DATABASE_PASSWORDS.getValue("TRANSFERS_DB_OWNER_PASSWORD"))

        val logDir = systemPath("e2e.log-dir")
        customer =
            ServiceProcess("customer", systemPath("e2e.customer-jar"), customerEnvironment(), emptyList(), logDir)
        accounts =
            ServiceProcess("accounts", systemPath("e2e.accounts-jar"), accountsEnvironment(), emptyList(), logDir)
        proxy = AccountsProxy(accounts.baseUrl)
        transfers =
            ServiceProcess("transfers", systemPath("e2e.transfers-jar"), transfersEnvironment(), SAGA_ARGUMENTS, logDir)
        notification =
            ServiceProcess(
                "notification",
                systemPath("e2e.notification-jar"),
                notificationEnvironment(),
                emptyList(),
                logDir,
            )
        statement =
            ServiceProcess("statement", systemPath("e2e.statement-jar"), statementEnvironment(), emptyList(), logDir)
        customer.start()
        accounts.start()
        transfers.start()
        notification.start()
        statement.start()
        customerApi = CustomerApiClient(customer.baseUrl, tokens)
        accountsApi = AccountsApiClient(accounts.baseUrl, customerApi, tokens)
        transfersApi = TransfersApiClient(transfers.baseUrl, tokens, accountsApi::ownerOf)
        notificationApi = NotificationApiClient(notification.baseUrl, tokens)
        statementApi = StatementApiClient(statement.baseUrl, tokens)
    }

    override fun close() {
        processes().forEach { runCatching { it.stop() } }
        if (::proxy.isInitialized) proxy.close()
        if (::sqs.isInitialized) sqs.close()
        if (::localStack.isInitialized) localStack.stop()
        postgres.stop()
        redis.stop()
        oauth.stop()
    }

    /** The issuer of every token: the services validate `iss` against it and the clients ask it for tokens. */
    private fun issuer() = "http://${oauth.host}:${oauth.getMappedPort(MOCK_OAUTH2_PORT)}/default"

    private fun customerEnvironment() =
        mapOf(
            "CUSTOMER_DB_URL" to jdbcUrl("customer"),
            "CUSTOMER_DB_USER" to "customer_app",
            "CUSTOMER_DB_PASSWORD" to DATABASE_PASSWORDS.getValue("CUSTOMER_DB_PASSWORD"),
            "CUSTOMER_DB_OWNER_USER" to "customer_owner",
            "CUSTOMER_DB_OWNER_PASSWORD" to DATABASE_PASSWORDS.getValue("CUSTOMER_DB_OWNER_PASSWORD"),
            "CUSTOMER_AWS_ENDPOINT" to localStack.endpoint.toString(),
            "CUSTOMER_AWS_REGION" to localStack.region,
            "CUSTOMER_TOPIC_ARN" to topicArn("customer-events.fifo"),
            "CUSTOMER_JWT_ISSUER_URI" to issuer(),
        )

    private fun accountsEnvironment() =
        mapOf(
            "ACCOUNTS_DB_URL" to jdbcUrl("accounts"),
            "ACCOUNTS_DB_USER" to "accounts_app",
            "ACCOUNTS_DB_PASSWORD" to DATABASE_PASSWORDS.getValue("ACCOUNTS_DB_PASSWORD"),
            "ACCOUNTS_DB_OWNER_USER" to "accounts_owner",
            "ACCOUNTS_DB_OWNER_PASSWORD" to DATABASE_PASSWORDS.getValue("ACCOUNTS_DB_OWNER_PASSWORD"),
            "ACCOUNTS_AWS_ENDPOINT" to localStack.endpoint.toString(),
            "ACCOUNTS_AWS_REGION" to localStack.region,
            "ACCOUNTS_TOPIC_ARN" to topicArn("accounts-events.fifo"),
            "ACCOUNTS_JWT_ISSUER_URI" to issuer(),
            "ACCOUNTS_OAUTH_TOKEN_URI" to "${issuer()}/token",
            "ACCOUNTS_CUSTOMER_BASE_URL" to customer.baseUrl,
        )

    private fun transfersEnvironment() =
        mapOf(
            "TRANSFERS_DB_URL" to jdbcUrl("transfers"),
            "TRANSFERS_DB_USER" to "transfers_app",
            "TRANSFERS_DB_PASSWORD" to DATABASE_PASSWORDS.getValue("TRANSFERS_DB_PASSWORD"),
            "TRANSFERS_DB_OWNER_USER" to "transfers_owner",
            "TRANSFERS_DB_OWNER_PASSWORD" to DATABASE_PASSWORDS.getValue("TRANSFERS_DB_OWNER_PASSWORD"),
            "TRANSFERS_ACCOUNTS_URL" to proxy.baseUrl,
            "TRANSFERS_AWS_ENDPOINT" to localStack.endpoint.toString(),
            "TRANSFERS_AWS_REGION" to localStack.region,
            "TRANSFERS_TOPIC_ARN" to topicArn("transfers-events.fifo"),
            "TRANSFERS_REDIS_HOST" to redis.host,
            "TRANSFERS_REDIS_PORT" to redis.getMappedPort(REDIS_PORT).toString(),
            "TRANSFERS_JWT_ISSUER_URI" to issuer(),
            "TRANSFERS_OAUTH_TOKEN_URI" to "${issuer()}/token",
            "TRANSFERS_CUSTOMER_BASE_URL" to customer.baseUrl,
        )

    private fun notificationEnvironment() =
        mapOf(
            "NOTIFICATION_DB_URL" to jdbcUrl("notification"),
            "NOTIFICATION_DB_USER" to "notification_app",
            "NOTIFICATION_DB_PASSWORD" to DATABASE_PASSWORDS.getValue("NOTIFICATION_DB_PASSWORD"),
            "NOTIFICATION_DB_OWNER_USER" to "notification_owner",
            "NOTIFICATION_DB_OWNER_PASSWORD" to DATABASE_PASSWORDS.getValue("NOTIFICATION_DB_OWNER_PASSWORD"),
            "NOTIFICATION_AWS_ENDPOINT" to localStack.endpoint.toString(),
            "NOTIFICATION_AWS_REGION" to localStack.region,
            "NOTIFICATION_JWT_ISSUER_URI" to issuer(),
        )

    private fun statementEnvironment() =
        mapOf(
            "STATEMENT_AWS_ENDPOINT" to localStack.endpoint.toString(),
            "STATEMENT_AWS_REGION" to localStack.region,
            "STATEMENT_REDIS_HOST" to redis.host,
            "STATEMENT_REDIS_PORT" to redis.getMappedPort(REDIS_PORT).toString(),
            // A long TTL (default 60s) makes the cache tests fail by invalidation, never by expiry. Spring relaxed binding
            // maps the variable to statement.cache.ttl.
            "STATEMENT_CACHE_TTL" to "600s",
            "STATEMENT_JWT_ISSUER_URI" to issuer(),
        )

    private fun jdbcUrl(database: String) = "jdbc:postgresql://${postgres.host}:${postgres.getMappedPort(POSTGRES_PORT)}/$database"

    private fun topicArn(name: String) = "arn:aws:sns:${localStack.region}:000000000000:$name"

    /** Waits for the ready.d scripts and fails if any of them did not succeed. */
    private fun awaitLocalStackInit() {
        val http = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder(URI.create("${localStack.endpoint}/_localstack/init/ready")).build()
        val deadline = Instant.now().plus(LOCALSTACK_INIT_TIMEOUT)
        while (Instant.now().isBefore(deadline)) {
            val status = mapper.readTree(http.send(request, HttpResponse.BodyHandlers.ofString()).body())
            if (status.get("completed").asBoolean()) {
                val scripts = status.get("scripts")
                val states = (0 until scripts.size()).map { scripts.get(it).get("state").asString() }
                check(states.isNotEmpty() && states.all { it == "SUCCESSFUL" }) {
                    "LocalStack init scripts did not succeed: $status\n${localStack.logs}"
                }
                return
            }
            Thread.sleep(INIT_POLL.toMillis())
        }
        error("LocalStack init scripts did not complete:\n${localStack.logs}")
    }

    companion object {
        /**
         * Saga times for the tests (defaults: deadline 2 min, lease 30 s, read 3 s, retry 1 s to 15 s).
         * - deadline 60 s: above lease (40 s) + restart (6 to 10 s) + margin, so a process killed mid-step resumes (when the
         *   lease ends) BEFORE the deadline and repeats the call; the tests of the kill assert that path. `accounts` also
         *   stops and starts again inside it. The one test that waits the deadline out (the lookup path, after it) takes
         *   about a minute, still inside SAGA_FINISH_TIMEOUT of 90 s;
         * - read timeout 10 s, so a call held at the proxy is still in flight when the test acts on it;
         * - call budget 13 s (connect 1 s + read 10 s + 2 s of the worst case of the machine token), the minimum
         *   SagaTimeoutsValidator accepts at startup; still well below the lease;
         * - lease 40 s: the lease test restarts `transfers` after a kill and needs it healthy before the lease ends. A
         *   restart takes 6 to 10 s unloaded; the margin (lease minus restart) was ~9.8 s with a 16 s lease and failed once on a
         *   loaded machine. At 40 s the margin is ~30 s, and the test only waits the remaining lease (well under
         *   SAGA_FINISH_TIMEOUT of 90 s);
         * - retries every 200 ms to 1 s and the scheduler every 200 ms, so a recovery is seen quickly.
         */
        val SAGA_ARGUMENTS =
            listOf(
                "--transfers.saga.deadline=60s",
                "--transfers.saga.lease=40s",
                "--transfers.saga.call-budget=13s",
                "--transfers.accounts.read-timeout=10s",
                "--transfers.saga.retry-initial=200ms",
                "--transfers.saga.retry-max=1s",
                "--transfers.saga.poll-interval=200ms",
                "--transfers.outbox.poll-interval=200ms",
            )

        /**
         * Read by the notification queues init script: a short visibility, so a message that always fails reaches the
         * DLQ after its receives (the script's default count) in about 10 s instead of 2.5 min.
         */
        private val NOTIFICATION_QUEUE_SETTINGS =
            mapOf(
                "NOTIFICATION_VISIBILITY_TIMEOUT_SECONDS" to "2",
            )

        private const val POSTGRES_IMAGE = "postgres:17-alpine"
        private const val POSTGRES_PORT = 5432
        private const val REDIS_IMAGE = "redis:7-alpine"
        private const val REDIS_PORT = 6379

        /** Same image as the compose (`mock-oauth2-server`); change both together. */
        private const val MOCK_OAUTH2_IMAGE = "ghcr.io/navikt/mock-oauth2-server:6.1.5"
        private const val MOCK_OAUTH2_PORT = 8080
        private const val MOCK_OAUTH2_CONFIG = "/config/mock-oauth2-config.json"
        private const val TRANSFERS_INSPECTION_QUEUE = "transfers-events-inspection.fifo"
        private const val EXECUTABLE = 0b111_101_101
        private val LOCALSTACK_INIT_TIMEOUT: Duration = Duration.ofSeconds(60)
        private val INIT_POLL: Duration = Duration.ofMillis(250)
        private val mapper = JsonMapper.builder().build()

        /** Read by the compose init script (`create_service_db`) and handed to the services. Test-only values. */
        private val DATABASE_PASSWORDS =
            mapOf(
                "ACCOUNTS_DB_OWNER_PASSWORD" to "accounts_owner",
                "ACCOUNTS_DB_PASSWORD" to "accounts_app",
                "TRANSFERS_DB_OWNER_PASSWORD" to "transfers_owner",
                "TRANSFERS_DB_PASSWORD" to "transfers_app",
                "NOTIFICATION_DB_OWNER_PASSWORD" to "notification_owner",
                "NOTIFICATION_DB_PASSWORD" to "notification_app",
                "CUSTOMER_DB_OWNER_PASSWORD" to "customer_owner",
                "CUSTOMER_DB_PASSWORD" to "customer_app",
            )

        private fun systemPath(property: String): Path =
            Path.of(checkNotNull(System.getProperty(property)) { "system property $property not set (run through Gradle)" })

        private fun scripts(directory: Path): List<Path> = Files.list(directory).use { files -> files.sorted().toList() }
    }
}
