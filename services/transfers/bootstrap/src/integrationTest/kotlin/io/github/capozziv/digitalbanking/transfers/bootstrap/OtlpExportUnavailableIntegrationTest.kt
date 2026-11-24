package io.github.capozziv.digitalbanking.transfers.bootstrap

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import io.github.capozziv.digitalbanking.transfers.bootstrap.web.ApiClient
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.ClassOrderer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestClassOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil

/**
 * A collector that is down or stuck neither fails nor slows a request: with the OTLP endpoint on a closed port, and
 * then on a socket that accepts the connection and never answers, 20 transfers in a row all conclude, their p95 stays
 * within [latencyLimit] of the p95 of the same requests against a receiver that accepts the export, and the log does
 * not get an exporter error or warning per request.
 *
 * The closed port alone does not tell a synchronous export apart (the refusal takes under 1 ms); the stuck collector
 * does: there each export waits [STUCK_EXPORT_TIMEOUT] before failing, so an export on the request thread would push
 * the p95 above [ABSOLUTE_P95_CEILING].
 *
 * The export is on for real (traces, metrics and logs, the defaults of `application.yml`); the build turns it off in
 * every other test. The receiver run goes first and leaves its p95 for the comparison, so the nested classes only run
 * together, in order (running one of the others alone fails on the missing baseline).
 */
@TestClassOrder(ClassOrderer.OrderAnnotation::class)
class OtlpExportUnavailableIntegrationTest : TransfersIntegrationTest() {
    @Nested
    @Order(1)
    @TestPropertySource(
        properties = [EXPORT_ON_TRACES, EXPORT_ON_METRICS, EXPORT_ON_LOGS, "$ENDPOINT=http://127.0.0.1:\${$RECEIVER_PORT_PROPERTY}"],
    )
    inner class WithAReceiver {
        @LocalServerPort
        private var port: Int = 0

        @Test
        fun `transfers conclude while the receiver accepts the export`() {
            val latencies = transfers(port)

            await().atMost(Duration.ofSeconds(20)).untilAsserted {
                assertThat(receiver.allServeEvents.map { it.request.url }).contains("/v1/traces", "/v1/logs", "/v1/metrics")
            }
            reachableP95 = p95(latencies)
        }
    }

    @Nested
    @Order(2)
    @ExtendWith(OutputCaptureExtension::class)
    @TestPropertySource(properties = [EXPORT_ON_TRACES, EXPORT_ON_METRICS, EXPORT_ON_LOGS, "$ENDPOINT=http://127.0.0.1:1"])
    inner class WithTheCollectorDown {
        @LocalServerPort
        private var port: Int = 0

        @Test
        fun `transfers conclude as fast and the log has no exporter error per request`(output: CapturedOutput) {
            val baseline = checkNotNull(reachableP95) { "the run with a receiver (WithAReceiver) must come first" }

            val latencies = transfers(port)
            // Every exporter tries at least once: logs every 1 s, spans every 5 s, metrics every 10 s.
            Thread.sleep(EXPORT_ATTEMPTS_WINDOW.toMillis())

            val p95 = p95(latencies)
            println("OTLP export p95: receiver $baseline, collector down $p95")
            assertFasterThanTheLimit(p95, baseline)
            // The export really ran and failed: the test is not passing because nothing was exported.
            assertThat(output.out).containsAnyOf("Failed to export", "Failed to publish metrics")
            val complaints = exporterComplaints(output)
            assertThat(complaints.filter { "\"level\":\"ERROR\"" in it })
                .describedAs(complaints.joinToString("\n"))
                .hasSizeLessThanOrEqualTo(MAX_EXPORTER_ERRORS)
            assertThat(complaints).describedAs(complaints.joinToString("\n")).hasSizeLessThan(REQUESTS)
        }
    }

    @Nested
    @Order(3)
    @ExtendWith(OutputCaptureExtension::class)
    @TestPropertySource(
        properties = [
            EXPORT_ON_TRACES, EXPORT_ON_METRICS, EXPORT_ON_LOGS, "$ENDPOINT=http://127.0.0.1:\${$STUCK_PORT_PROPERTY}",
            "management.opentelemetry.tracing.export.otlp.timeout=$STUCK_EXPORT_TIMEOUT_PROPERTY",
            "management.opentelemetry.logging.export.otlp.timeout=$STUCK_EXPORT_TIMEOUT_PROPERTY",
            "management.otlp.metrics.export.read-timeout=$STUCK_EXPORT_TIMEOUT_PROPERTY",
        ],
    )
    inner class WithAStuckCollector {
        @LocalServerPort
        private var port: Int = 0

        @Test
        fun `transfers conclude as fast while the collector accepts and never answers`(output: CapturedOutput) {
            val baseline = checkNotNull(reachableP95) { "the run with a receiver (WithAReceiver) must come first" }
            // Only then can a synchronous export (at least one timeout per request) break the ceiling.
            assertThat(ABSOLUTE_P95_CEILING).isLessThan(STUCK_EXPORT_TIMEOUT)

            val latencies = transfers(port)
            Thread.sleep(EXPORT_ATTEMPTS_WINDOW.toMillis())

            val p95 = p95(latencies)
            println("OTLP export p95: receiver $baseline, collector stuck $p95")
            assertFasterThanTheLimit(p95, baseline)
            // The exporters reached the stuck collector: the test is not passing because nothing was exported.
            assertThat(stuckCollector.connections.get()).isPositive()
            val complaints = exporterComplaints(output)
            assertThat(complaints).describedAs(complaints.joinToString("\n")).hasSizeLessThan(REQUESTS)
        }
    }

    /**
     * The p95 stays under twice the [baseline], with at least [LATENCY_MARGIN] of room against CI noise on a fast
     * baseline, and under [ABSOLUTE_P95_CEILING] whatever the baseline.
     */
    private fun assertFasterThanTheLimit(
        p95: Duration,
        baseline: Duration,
    ) {
        assertThat(p95).describedAs("p95 (receiver: $baseline)").isLessThan(latencyLimit(baseline))
        assertThat(p95).describedAs("p95 (receiver: $baseline)").isLessThan(ABSOLUTE_P95_CEILING)
    }

    private fun latencyLimit(baseline: Duration): Duration = maxOf(baseline.multipliedBy(2), baseline.plus(LATENCY_MARGIN))

    /** Exporter lines at WARN or ERROR (`OtlpMeterRegistry` warns, the OpenTelemetry exporters log SEVERE as ERROR). */
    private fun exporterComplaints(output: CapturedOutput): List<String> =
        output.out.lines().filter { line ->
            listOf("\"level\":\"ERROR\"", "\"level\":\"WARN\"").any { it in line } &&
                listOf("otlp", "export").any { line.contains(it, ignoreCase = true) }
        }

    /** [WARM_UP] requests, then the [REQUESTS] whose latencies count; all must answer 201 or 202, none slower than [SINGLE_REQUEST_CEILING]. */
    private fun transfers(port: Int): List<Duration> {
        val api = ApiClient(port)
        val source = UUID.randomUUID()
        val destination = UUID.randomUUID()
        stubAccounts(source, destination)
        val body = """{"sourceAccountId":"$source","destinationAccountId":"$destination","amount":100}"""
        val latencies =
            (1..WARM_UP + REQUESTS).map {
                val started = System.nanoTime()
                val response = api.post("/transfers", body, mapOf("Idempotency-Key" to UUID.randomUUID().toString()))
                val elapsed = Duration.ofNanos(System.nanoTime() - started)
                assertThat(response.status).describedAs(response.rawBody).isIn(201, 202)
                // Fails at the first request that waits for an export, before the 5-minute test token expires.
                assertThat(elapsed).describedAs("request $it").isLessThan(SINGLE_REQUEST_CEILING)
                elapsed
            }
        return latencies.drop(WARM_UP)
    }

    private fun stubAccounts(vararg ids: UUID) {
        ids.forEach { id ->
            accountsServer.stubFor(
                get(urlEqualTo("/accounts/$id")).willReturn(
                    json(
                        200,
                        """{"id":"$id","customerId":"$CUSTOMER_ID","type":"FREE","balance":0,"overdraftLimit":0,"availableBalance":0}""",
                    ),
                ),
            )
            accountsServer.stubFor(
                post(urlPathMatching("/accounts/$id/(debits|credits)")).willReturn(
                    json(
                        201,
                        """{"id":"${UUID.randomUUID()}","accountId":"$id","sequence":1,"kind":"DEBIT","amount":100,""" +
                            """"balanceAfter":0,"createdAt":"2026-10-27T10:00:00Z"}""",
                    ),
                ),
            )
        }
    }

    private fun json(
        status: Int,
        body: String,
    ) = aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody(body)

    private fun p95(latencies: List<Duration>): Duration = latencies.sorted()[ceil(latencies.size * 0.95).toInt() - 1]

    companion object {
        const val ENDPOINT = "digitalbanking.observability.otlp-endpoint"
        const val RECEIVER_PORT_PROPERTY = "otlp-export-test.receiver-port"
        const val EXPORT_ON_TRACES = "management.tracing.export.otlp.enabled=true"
        const val EXPORT_ON_METRICS = "management.otlp.metrics.export.enabled=true"
        const val EXPORT_ON_LOGS = "management.logging.export.otlp.enabled=true"
        const val WARM_UP = 5
        const val REQUESTS = 20

        /** One failed export per signal (traces, metrics, logs) may be logged; never one per request. */
        const val MAX_EXPORTER_ERRORS = 3
        val EXPORT_ATTEMPTS_WINDOW: Duration = Duration.ofSeconds(11)

        /**
         * How long each export waits on the stuck collector. Shorter than the 10 s default only so that a regression
         * (an export on the request thread) fails in minutes and not in an hour; any value above
         * [ABSOLUTE_P95_CEILING] tells a synchronous export apart.
         */
        val STUCK_EXPORT_TIMEOUT: Duration = Duration.ofSeconds(2)
        const val STUCK_EXPORT_TIMEOUT_PROPERTY = "2s"

        /**
         * Against noise on a fast baseline (60 to 200 ms here): twice 60 ms would leave a GC pause no room. 250 ms is
         * still well under the [STUCK_EXPORT_TIMEOUT] a single synchronous export would add.
         */
        val LATENCY_MARGIN: Duration = Duration.ofMillis(250)

        /**
         * Whatever the baseline, the p95 of a transfer with a stubbed `accounts` stays under 1 s: five times the slowest
         * baseline measured, and half of what one synchronous export to the stuck collector would add.
         */
        val ABSOLUTE_P95_CEILING: Duration = Duration.ofSeconds(1)

        /** No single request comes near it (they take 40 to 200 ms); one export on the request thread goes past it. */
        val SINGLE_REQUEST_CEILING: Duration = Duration.ofSeconds(5)
        const val STUCK_PORT_PROPERTY = "otlp-export-test.stuck-port"

        /** Accepts every OTLP/HTTP export (`/v1/traces`, `/v1/metrics`, `/v1/logs`) with an empty 200. */
        @JvmStatic
        val receiver: WireMockServer =
            WireMockServer(options().dynamicPort()).apply {
                start()
                stubFor(post(urlPathMatching("/v1/.*")).willReturn(aResponse().withStatus(200)))
                // Read by the endpoint placeholder of WithAReceiver; set before any context of this class starts.
                System.setProperty(RECEIVER_PORT_PROPERTY, port().toString())
            }

        /** Accepts every connection and never answers: an OTLP collector that hangs. */
        @JvmStatic
        val stuckCollector: StuckCollector =
            StuckCollector().also {
                // Read by the endpoint placeholder of WithAStuckCollector; set before any context of this class starts.
                System.setProperty(STUCK_PORT_PROPERTY, it.port.toString())
            }

        @Volatile
        private var reachableP95: Duration? = null
    }

    /** A server socket whose accepted connections stay open and silent until the JVM ends. */
    class StuckCollector {
        private val server = ServerSocket(0, BACKLOG, InetAddress.getLoopbackAddress())
        private val open = CopyOnWriteArrayList<Socket>()
        val connections = AtomicInteger()
        val port: Int = server.localPort

        init {
            Thread.ofPlatform().daemon().name("stuck-otlp-collector").start {
                while (!server.isClosed) {
                    open += server.accept()
                    connections.incrementAndGet()
                }
            }
        }

        private companion object {
            const val BACKLOG = 50
        }
    }
}
