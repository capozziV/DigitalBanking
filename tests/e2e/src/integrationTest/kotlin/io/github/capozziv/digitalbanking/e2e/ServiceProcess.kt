package io.github.capozziv.digitalbanking.e2e

import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.APPEND
import java.nio.file.StandardOpenOption.CREATE
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * One service running as `java -jar` with its own environment and arguments, on a port chosen once, so restarts keep the URL.
 * Each start appends to `<logDir>/<name>.log`; [logTail] goes into the report of a failed test.
 */
class ServiceProcess(
    val name: String,
    private val jar: Path,
    private val environment: Map<String, String>,
    private val arguments: List<String>,
    private val logDir: Path,
    private val healthTimeout: Duration = Duration.ofSeconds(90),
) : AutoCloseable {
    val port: Int = freePort()
    val baseUrl: String = "http://127.0.0.1:$port"
    private val logFile: Path = logDir.resolve("$name.log")
    private var process: Process? = null
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()

    val isRunning: Boolean get() = process?.isAlive == true

    /** Starts the process and returns once `/actuator/health` answers `UP`; fails with the log tail otherwise. */
    fun start() {
        check(!isRunning) { "$name is already running" }
        Files.createDirectories(logDir)
        Files.writeString(logFile, "\n===== start of $name at ${Instant.now()} =====\n", CREATE, APPEND)
        val command = listOf(javaExecutable(), "-jar", jar.toString(), "--server.port=$port") + arguments
        val builder =
            ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()))
        builder.environment().putAll(environment)
        val started = builder.start()
        process = started
        awaitHealthy(started)
    }

    fun ensureRunning() {
        if (!isRunning) start()
    }

    /** Graceful stop (SIGTERM); forced after [STOP_TIMEOUT]. */
    fun stop() {
        val running = process ?: return
        running.destroy()
        if (!running.waitFor(STOP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
            running.destroyForcibly().waitFor(STOP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
        }
        process = null
    }

    /** Abrupt death (SIGKILL): no shutdown hook, no lease released, requests in flight cut. */
    fun kill() {
        val running = process ?: return
        running.destroyForcibly()
        check(running.waitFor(STOP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) { "$name did not die after SIGKILL" }
        process = null
    }

    fun restart() {
        stop()
        start()
    }

    fun logTail(lines: Int = LOG_TAIL_LINES): String {
        if (!Files.exists(logFile)) return "(no log for $name)"
        val all = Files.readAllLines(logFile)
        return all.takeLast(lines).joinToString("\n", prefix = "----- last ${minOf(lines, all.size)} lines of $logFile -----\n")
    }

    override fun close() = stop()

    private fun awaitHealthy(started: Process) {
        val request = HttpRequest.newBuilder(URI.create("$baseUrl/actuator/health")).timeout(Duration.ofSeconds(2)).build()
        val deadline = Instant.now().plus(healthTimeout)
        while (Instant.now().isBefore(deadline)) {
            check(started.isAlive) { "$name exited with ${started.exitValue()} before becoming healthy\n${logTail()}" }
            val healthy =
                runCatching { http.send(request, HttpResponse.BodyHandlers.ofString()) }
                    .map { it.statusCode() == 200 && it.body().contains("\"status\":\"UP\"") }
                    .getOrDefault(false)
            if (healthy) return
            Thread.sleep(HEALTH_POLL.toMillis())
        }
        started.destroyForcibly()
        process = null
        error("$name was not healthy within $healthTimeout\n${logTail()}")
    }

    private companion object {
        val STOP_TIMEOUT: Duration = Duration.ofSeconds(20)
        val HEALTH_POLL: Duration = Duration.ofMillis(200)
        const val LOG_TAIL_LINES = 200

        /** The JVM running the tests (the Gradle toolchain's JDK 21) also runs the services. */
        fun javaExecutable(): String =
            ProcessHandle
                .current()
                .info()
                .command()
                .orElse("java")

        fun freePort(): Int = ServerSocket(0).use { it.localPort }
    }
}
