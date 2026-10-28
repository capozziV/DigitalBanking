package io.github.capozziv.digitalbanking.e2e

import io.github.capozziv.digitalbanking.testing.localstack.LocalStackTestSupport
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
 * Everything the end-to-end tests run against, as in the local environment: one PostgreSQL with the `accounts` and
 * `transfers` databases and their owner/app users (created by the compose init script itself), LocalStack with the
 * topics and inspection queues of the compose init scripts, both services as `java -jar` processes, and
 * [AccountsProxy] between `transfers` and `accounts`.
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
    private lateinit var localStack: LocalStackContainer
    private lateinit var sqs: SqsClient

    lateinit var accounts: ServiceProcess
        private set
    lateinit var transfers: ServiceProcess
        private set
    lateinit var proxy: AccountsProxy
        private set
    lateinit var accountsApi: AccountsApiClient
        private set
    lateinit var transfersApi: TransfersApiClient
        private set
    lateinit var transfersDatabase: TransfersDatabase
        private set
    lateinit var transferEvents: TransferEventsQueue
        private set

    fun processes(): List<ServiceProcess> =
        listOfNotNull(if (::accounts.isInitialized) accounts else null) +
            listOfNotNull(if (::transfers.isInitialized) transfers else null)

    /** Starts LocalStack first: without the token the whole class is skipped locally (and fails on CI) right away. */
    fun start() {
        localStack = LocalStackTestSupport.container(listOf("sns", "sqs"))
        scripts(systemPath("e2e.localstack-init-dir")).forEach { script ->
            localStack.withCopyFileToContainer(
                MountableFile.forHostPath(script, EXECUTABLE),
                "/etc/localstack/init/ready.d/${script.fileName}",
            )
        }
        localStack.start()
        awaitLocalStackInit()
        postgres.start()

        sqs = LocalStackTestSupport.sqsClient(localStack)
        transferEvents = TransferEventsQueue(sqs, sqs.getQueueUrl { it.queueName(TRANSFERS_INSPECTION_QUEUE) }.queueUrl())
        transfersDatabase =
            TransfersDatabase(jdbcUrl("transfers"), "transfers_owner", DATABASE_PASSWORDS.getValue("TRANSFERS_DB_OWNER_PASSWORD"))

        val logDir = systemPath("e2e.log-dir")
        accounts =
            ServiceProcess("accounts", systemPath("e2e.accounts-jar"), accountsEnvironment(), emptyList(), logDir)
        proxy = AccountsProxy(accounts.baseUrl)
        transfers =
            ServiceProcess("transfers", systemPath("e2e.transfers-jar"), transfersEnvironment(), SAGA_ARGUMENTS, logDir)
        accounts.start()
        transfers.start()
        accountsApi = AccountsApiClient(accounts.baseUrl)
        transfersApi = TransfersApiClient(transfers.baseUrl)
    }

    override fun close() {
        processes().forEach { runCatching { it.stop() } }
        if (::proxy.isInitialized) proxy.close()
        if (::sqs.isInitialized) sqs.close()
        if (::localStack.isInitialized) localStack.stop()
        postgres.stop()
    }

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
         * - deadline 30 s: `accounts` stops and starts again inside it (a restart takes 6 to 10 s on a laptop, so a slower CI
         *   runner still has room), and the test that waits it out stays well under a minute;
         * - read timeout 10 s, so a call held at the proxy is still in flight when the test acts on it;
         * - call budget 11 s (connect 1 s + read 10 s) below the lease of 16 s, as the saga requires;
         * - retries every 200 ms to 1 s and the scheduler every 200 ms, so a recovery is seen quickly.
         */
        val SAGA_ARGUMENTS =
            listOf(
                "--transfers.saga.deadline=30s",
                "--transfers.saga.lease=16s",
                "--transfers.saga.call-budget=11s",
                "--transfers.accounts.read-timeout=10s",
                "--transfers.saga.retry-initial=200ms",
                "--transfers.saga.retry-max=1s",
                "--transfers.saga.poll-interval=200ms",
                "--transfers.outbox.poll-interval=200ms",
            )

        private const val POSTGRES_IMAGE = "postgres:17-alpine"
        private const val POSTGRES_PORT = 5432
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
            )

        private fun systemPath(property: String): Path =
            Path.of(checkNotNull(System.getProperty(property)) { "system property $property not set (run through Gradle)" })

        private fun scripts(directory: Path): List<Path> = Files.list(directory).use { files -> files.sorted().toList() }
    }
}
