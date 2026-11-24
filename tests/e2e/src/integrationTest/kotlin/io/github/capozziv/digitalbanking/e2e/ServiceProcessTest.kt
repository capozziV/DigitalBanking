package io.github.capozziv.digitalbanking.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path

/** The environment of a service process, checked without starting it. */
class ServiceProcessTest {
    @Test
    fun `every service process starts with the OTLP export turned off`() {
        val process = ServiceProcess("accounts", Path.of("accounts.jar"), mapOf("SOME_SETTING" to "1"), emptyList(), Path.of("logs"))

        // The same three switches the build gives every Test task (relaxed binding of the environment): a collector
        // of the local stack never gets e2e data.
        assertThat(process.processEnvironment()).containsAllEntriesOf(
            mapOf(
                "MANAGEMENT_TRACING_EXPORT_OTLP_ENABLED" to "false",
                "MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED" to "false",
                "MANAGEMENT_LOGGING_EXPORT_OTLP_ENABLED" to "false",
                "SOME_SETTING" to "1",
            ),
        )
    }

    @Test
    fun `the environment of the test can turn the export back on`() {
        val process =
            ServiceProcess(
                "accounts",
                Path.of("accounts.jar"),
                mapOf("MANAGEMENT_TRACING_EXPORT_OTLP_ENABLED" to "true"),
                emptyList(),
                Path.of("logs"),
            )

        assertThat(process.processEnvironment()).containsEntry("MANAGEMENT_TRACING_EXPORT_OTLP_ENABLED", "true")
    }
}
