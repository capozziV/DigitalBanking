package io.github.capozziv.digitalbanking.observability

import io.micrometer.tracing.Tracer
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.data.SpanData
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@SpringBootTest(
    classes = [TracingAutoConfigurationTest.ProbeApplication::class],
    properties = [
        "management.tracing.sampling.probability=1.0",
        // As in the application.yml of every service.
        "logging.structured.format.console=ecs",
        // Spring Cloud AWS SQS is on this test classpath only for the SQS interceptor tests.
        "spring.cloud.aws.sqs.enabled=false",
        // JDBC comes with :libs:outbox, on this test classpath only to compare the MDC key.
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
    ],
)
@AutoConfigureMockMvc
@AutoConfigureTracing
@ExtendWith(OutputCaptureExtension::class)
class TracingAutoConfigurationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var spans: InMemorySpanExporter

    @Autowired
    lateinit var logs: InMemoryLogRecordExporter

    @Autowired
    lateinit var tracer: Tracer

    @BeforeEach
    fun clear() {
        spans.reset()
        logs.reset()
    }

    @Test
    fun `tracer bean exists and http server spans carry correlation id attribute`() {
        assertThat(tracer).isNotNull()

        mockMvc.get("/probe") { header(CorrelationId.HEADER, "abc") }.andExpect { status { isOk() } }

        val span = awaitServerSpan()
        assertThat(span.attributes.asMap().mapKeys { it.key.key }).containsEntry("correlation.id", "abc")
    }

    @Test
    fun `server span does not record query string or authorization header`() {
        mockMvc
            .get("/probe?phone=11999998888") {
                header("Authorization", "Bearer $SECRET_TOKEN")
                header(CorrelationId.HEADER, "def")
            }.andExpect { status { isOk() } }

        val attributes = awaitServerSpan().attributes.asMap().mapKeys { it.key.key }
        assertThat(attributes).doesNotContainKey("url.query")
        assertThat(attributes.values.map { it.toString() }).noneMatch { SECRET_TOKEN in it || "11999998888" in it }
    }

    @Test
    fun `log lines are exported by OTLP with the trace context and the correlation id`() {
        mockMvc.get("/probe") { header(CorrelationId.HEADER, "ghi") }.andExpect { status { isOk() } }

        val span = awaitServerSpan()
        val record =
            await()
                .atMost(
                    Duration.ofSeconds(10),
                ).until({ logs.finishedLogRecordItems.firstOrNull { it.bodyValue?.asString() == PROBE_LOG } }) {
                    it != null
                }!!
        assertThat(record.spanContext.traceId).isEqualTo(span.traceId)
        assertThat(record.attributes.asMap().mapKeys { it.key.key }).containsEntry(CorrelationId.MDC_KEY, "ghi")
    }

    @Test
    fun `the console log line is JSON with traceId, spanId and correlationId`(output: CapturedOutput) {
        mockMvc.get("/probe") { header(CorrelationId.HEADER, "jkl") }.andExpect { status { isOk() } }

        val span = awaitServerSpan()
        val line = output.out.lines().single { "\"$PROBE_LOG\"" in it && "\"correlationId\":\"jkl\"" in it }
        assertThat(line).startsWith("{").contains("\"traceId\":\"${span.traceId}\"").contains("\"spanId\":\"")
    }

    private fun awaitServerSpan(): SpanData =
        await().atMost(Duration.ofSeconds(10)).until({ spans.finishedSpanItems.firstOrNull { it.name.contains("/probe") } }) {
            it != null
        }!!

    /** No component scan: it would load the auto-configurations of this package as user configuration, out of order. */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    class ProbeApplication {
        @Bean
        fun inMemorySpanExporter(): InMemorySpanExporter = InMemorySpanExporter.create()

        @Bean
        fun inMemoryLogRecordExporter(): InMemoryLogRecordExporter = InMemoryLogRecordExporter.create()

        @Bean
        fun probeController() = ProbeController()
    }

    @RestController
    class ProbeController {
        @GetMapping("/probe")
        fun probe(): String {
            LoggerFactory.getLogger(ProbeController::class.java).info(PROBE_LOG)
            return "ok"
        }
    }

    private companion object {
        const val SECRET_TOKEN = "secret-token-value"
        const val PROBE_LOG = "probe answered"
    }
}
