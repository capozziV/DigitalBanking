import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("digitalbanking.kotlin-library")
    kotlin("plugin.spring")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(platform(libs.findLibrary("spring-boot-bom").get()))
    testImplementation(platform(libs.findLibrary("spring-boot-bom").get()))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

// Mockito as an explicit -javaagent (the setup Mockito recommends for JDK 21+). Without it, Mockito attaches its
// inline mock maker dynamically and the JVM warns about dynamic agent loading and class data sharing.
val mockitoAgent: Configuration by configurations.creating

dependencies {
    mockitoAgent(platform(libs.findLibrary("spring-boot-bom").get()))
    mockitoAgent("org.mockito:mockito-core") { isTransitive = false }
}

abstract class MockitoAgentArgumentProvider : CommandLineArgumentProvider {
    @get:Classpath
    abstract val agent: ConfigurableFileCollection

    override fun asArguments() = listOf("-javaagent:${agent.singleFile.absolutePath}")
}

tasks.withType<Test>().configureEach {
    jvmArgumentProviders.add(
        objects.newInstance<MockitoAgentArgumentProvider>().apply { agent.from(mockitoAgent) },
    )
}
