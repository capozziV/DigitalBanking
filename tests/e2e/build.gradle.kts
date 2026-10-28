import java.time.Duration

// End-to-end tests of the transfer saga: accounts and transfers run as real processes (java -jar of the boot jars),
// with PostgreSQL and LocalStack in Testcontainers. Everything lives in the integrationTest source set.
plugins {
    id("digitalbanking.integration-test")
}

dependencies {
    integrationTestImplementation(project(":libs:testing-localstack"))
    integrationTestImplementation(platform(libs.spring.boot.bom))
    integrationTestImplementation(libs.testcontainers.postgresql)
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
    integrationTestImplementation(libs.postgresql)
    integrationTestImplementation("tools.jackson.core:jackson-databind")
}

// The jars and the init scripts reach the tests as system properties. They are task inputs (the jar content, the
// scripts by relative path), so a change in either service reruns the tests and the clone path stays out of the cache key.
abstract class EndToEndInputs : CommandLineArgumentProvider {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val accountsJar: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val transfersJar: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val postgresInitDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val localStackInitDir: DirectoryProperty

    @get:Internal
    abstract val logDir: DirectoryProperty

    override fun asArguments() =
        listOf(
            "-De2e.accounts-jar=${accountsJar.get().asFile.absolutePath}",
            "-De2e.transfers-jar=${transfersJar.get().asFile.absolutePath}",
            "-De2e.postgres-init-dir=${postgresInitDir.get().asFile.absolutePath}",
            "-De2e.localstack-init-dir=${localStackInitDir.get().asFile.absolutePath}",
            "-De2e.log-dir=${logDir.get().asFile.absolutePath}",
        )
}

tasks.named<Test>("integrationTest") {
    dependsOn(":services:accounts:bootstrap:bootJar", ":services:transfers:bootstrap:bootJar")
    // Two service processes and their restarts: room for the suite, but a stuck process never hangs the build.
    timeout = Duration.ofMinutes(15)
    jvmArgumentProviders.add(
        objects.newInstance<EndToEndInputs>().apply {
            val root = rootProject.layout.projectDirectory
            accountsJar.set(root.file("services/accounts/bootstrap/build/libs/bootstrap.jar"))
            transfersJar.set(root.file("services/transfers/bootstrap/build/libs/bootstrap.jar"))
            postgresInitDir.set(root.dir("infra/local/postgres/init"))
            localStackInitDir.set(root.dir("infra/local/localstack/init/ready.d"))
            logDir.set(layout.buildDirectory.dir("e2e-logs"))
        },
    )
}
