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
import java.time.Duration
import java.util.UUID
import kotlin.math.ceil

/**
 * A collector that is down neither fails nor slows a request: with the OTLP endpoint on a closed port, 20 transfers in
 * a row all conclude, their p95 stays under twice the p95 of the same requests against a receiver that accepts the
 * export, and the log does not get an exporter error per request.
 *
 * The export is on for real (traces, metrics and logs, the defaults of `application.yml`); the build turns it off in
 * every other test. The receiver run goes first and leaves its p95 for the comparison.
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
            assertThat(p95).describedAs("p95 with the collector down (receiver: $baseline)").isLessThan(baseline.multipliedBy(2))
            // The export really ran and failed: the test is not passing because nothing was exported.
            assertThat(output.out).containsAnyOf("Failed to export", "Failed to publish metrics")
            val exporterErrors =
                output.out.lines().filter { line ->
                    "\"level\":\"ERROR\"" in line && listOf("otlp", "export").any { line.contains(it, ignoreCase = true) }
                }
            assertThat(exporterErrors).describedAs(exporterErrors.joinToString("\n")).hasSizeLessThanOrEqualTo(MAX_EXPORTER_ERRORS)
        }
    }

    /** [WARM_UP] requests, then [REQUESTS] timed ones; all must answer 201 or 202. */
    private fun transfers(port: Int): List<Duration> {
        val api = ApiClient(port)
        val source = UUID.randomUUID()
        val destination = UUID.randomUUID()
        stubAccounts(source, destination)
        val body = """{"sourceAccountId":"$source","destinationAccountId":"$destination","amount":100}"""
        repeat(WARM_UP) { api.post("/transfers", body, mapOf("Idempotency-Key" to UUID.randomUUID().toString())) }
        return (1..REQUESTS).map {
            val started = System.nanoTime()
            val response = api.post("/transfers", body, mapOf("Idempotency-Key" to UUID.randomUUID().toString()))
            val elapsed = Duration.ofNanos(System.nanoTime() - started)
            assertThat(response.status).describedAs(response.rawBody).isIn(201, 202)
            elapsed
        }
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

        /** Accepts every OTLP/HTTP export (`/v1/traces`, `/v1/metrics`, `/v1/logs`) with an empty 200. */
        @JvmStatic
        val receiver: WireMockServer =
            WireMockServer(options().dynamicPort()).apply {
                start()
                stubFor(post(urlPathMatching("/v1/.*")).willReturn(aResponse().withStatus(200)))
                // Read by the endpoint placeholder of WithAReceiver; set before any context of this class starts.
                System.setProperty(RECEIVER_PORT_PROPERTY, port().toString())
            }

        @Volatile
        private var reachableP95: Duration? = null
    }
}
