package io.github.capozziv.digitalbanking.observability

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean

/**
 * Sends every log line also by OTLP: attaches the OpenTelemetry Logback appender to the root logger and points it at
 * the [openTelemetry] of the context, whose batch processor (bounded queue, drops when full) does the export, so a
 * collector that is down never blocks the thread that logs. The record carries the trace context of the line and the
 * correlation ID as the `correlationId` attribute; the JSON on stdout stays as it is.
 *
 * Attached in code instead of a `logback-spring.xml` per service. Attaching twice (two contexts in one JVM, as in the
 * tests) keeps one appender; the context that closes detaches it.
 */
class OtlpLogAppender(
    private val openTelemetry: OpenTelemetry,
) : InitializingBean,
    DisposableBean {
    override fun afterPropertiesSet() {
        val root = rootLogger() ?: return
        if (root.getAppender(APPENDER_NAME) == null) {
            val appender =
                OpenTelemetryAppender().apply {
                    name = APPENDER_NAME
                    context = root.loggerContext
                    setCaptureMdcAttributes(CorrelationId.MDC_KEY)
                    start()
                }
            root.addAppender(appender)
        }
        OpenTelemetryAppender.install(openTelemetry)
    }

    override fun destroy() {
        rootLogger()?.getAppender(APPENDER_NAME)?.let { appender ->
            rootLogger()?.detachAppender(appender)
            appender.stop()
        }
    }

    /** Null when Logback is not the SLF4J binding: there is then nothing to attach to. */
    private fun rootLogger(): Logger? = (LoggerFactory.getILoggerFactory() as? LoggerContext)?.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)

    companion object {
        const val APPENDER_NAME = "OTLP"
    }
}
