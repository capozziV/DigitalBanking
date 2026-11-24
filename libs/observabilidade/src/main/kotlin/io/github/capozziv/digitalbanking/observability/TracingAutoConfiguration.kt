package io.github.capozziv.digitalbanking.observability

import io.opentelemetry.api.OpenTelemetry
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Tracing of every service. The OpenTelemetry SDK, the W3C propagation, the OTLP exporters (in batches, with a
 * bounded queue) and the HTTP server spans come from Spring Boot (`spring-boot-starter-opentelemetry`, configured in
 * each `application.yml`); this adds what is ours:
 * - [CorrelationIdSpanAttribute], the correlation ID as the `correlation.id` span attribute;
 * - [OtlpLogAppender], so the log lines also go out by OTLP, besides the JSON on stdout.
 *
 * The `RestClient` of each call between services is traced where it is built (see [traced]).
 */
@AutoConfiguration(afterName = [OPEN_TELEMETRY_SDK_AUTO_CONFIGURATION])
@ConditionalOnClass(name = ["io.micrometer.observation.ObservationFilter"])
class TracingAutoConfiguration {
    @Bean
    fun correlationIdSpanAttribute(): CorrelationIdSpanAttribute = CorrelationIdSpanAttribute()

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(
        name = [
            "ch.qos.logback.classic.LoggerContext",
            "io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender",
        ],
    )
    @ConditionalOnBean(OpenTelemetry::class)
    class OtlpLogs {
        @Bean
        fun otlpLogAppender(openTelemetry: OpenTelemetry): OtlpLogAppender = OtlpLogAppender(openTelemetry)
    }
}

private const val OPEN_TELEMETRY_SDK_AUTO_CONFIGURATION =
    "org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration"
