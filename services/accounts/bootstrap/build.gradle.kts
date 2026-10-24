plugins { id("digitalbanking.spring-boot-app") }

dependencies {
    implementation(project(":services:accounts:adapters"))
    implementation(project(":services:accounts:application"))
    implementation(project(":services:accounts:domain"))
    implementation(project(":libs:outbox"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation(libs.archunit)
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    integrationTestImplementation(project(":libs:testing-localstack"))
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-jdbc")
    integrationTestImplementation(libs.testcontainers.postgresql)
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
}

// OutboxRelayIntegrationTest runs LocalStack with the same init scripts as the local compose. The directory is a
// relative-path input, so the absolute path of the clone does not enter the build cache key.
abstract class LocalStackInitScripts : CommandLineArgumentProvider {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val directory: DirectoryProperty

    override fun asArguments() = listOf("-Daccounts.localstack.init-dir=${directory.get().asFile.absolutePath}")
}

tasks.named<Test>("integrationTest") {
    jvmArgumentProviders.add(
        objects.newInstance<LocalStackInitScripts>().apply {
            directory.set(rootProject.layout.projectDirectory.dir("infra/local/localstack/init/ready.d"))
        },
    )
}
