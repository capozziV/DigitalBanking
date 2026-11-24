import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors = true
    }
}

ktlint {
    version = libs.findVersion("ktlint").get().requiredVersion
}

dependencies {
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.findLibrary("assertj-core").get())
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// No test exports telemetry to a collector that may be running on the machine (OTLP on 127.0.0.1:4318, the default of
// the services). Spans, metrics and logs are still produced; a test that checks the OTLP export turns it back on with
// its own properties, which win over system properties.
tasks.withType<Test>().configureEach {
    systemProperty("management.tracing.export.otlp.enabled", "false")
    systemProperty("management.otlp.metrics.export.enabled", "false")
    systemProperty("management.logging.export.otlp.enabled", "false")
}
