package io.github.capozziv.digitalbanking.e2e

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler
import org.opentest4j.TestAbortedException

/**
 * Attaches the log tail of each service process to the failure of a test (or of its setup), so the report shows what
 * the services were doing. The full logs stay under `build/e2e-logs/`.
 */
class ProcessLogsOnFailureExtension(
    private val processes: () -> List<ServiceProcess>,
) : TestExecutionExceptionHandler,
    LifecycleMethodExecutionExceptionHandler {
    override fun handleTestExecutionException(
        context: ExtensionContext,
        throwable: Throwable,
    ) = throw withLogs(throwable)

    override fun handleBeforeAllMethodExecutionException(
        context: ExtensionContext,
        throwable: Throwable,
    ) = throw withLogs(throwable)

    override fun handleBeforeEachMethodExecutionException(
        context: ExtensionContext,
        throwable: Throwable,
    ) = throw withLogs(throwable)

    private fun withLogs(throwable: Throwable): Throwable =
        if (throwable is TestAbortedException) {
            throwable
        } else {
            throwable.apply {
                processes().forEach { process -> addSuppressed(ServiceLog("${process.name} log\n${process.logTail()}")) }
            }
        }

    /** Carries a log tail in the report; not an error of its own. */
    private class ServiceLog(
        log: String,
    ) : RuntimeException(log, null, false, false)
}
