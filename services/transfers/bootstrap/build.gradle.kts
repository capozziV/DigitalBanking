plugins { id("digitalbanking.spring-boot-app") }

dependencies {
    implementation(project(":services:transfers:adapters"))
    implementation(project(":services:transfers:domain"))
    implementation(project(":services:transfers:application"))
    implementation(project(":libs:outbox"))
    implementation(project(":libs:outbox-relay"))
    // SagaTimeoutsValidator adds the worst case of the machine token to the call budget.
    implementation(project(":libs:security"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    testImplementation(project(":libs:testing-architecture"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    integrationTestImplementation(testFixtures(project(":libs:outbox")))
    integrationTestImplementation(project(":libs:testing-localstack"))
    integrationTestImplementation(project(":libs:testing-redis"))
    integrationTestImplementation(project(":libs:testing-security"))
    integrationTestImplementation("org.awaitility:awaitility") // version managed by the Spring Boot BOM
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-jdbc")
    // V6BackfillIntegrationTest migrates a database of its own step by step.
    integrationTestImplementation("org.flywaydb:flyway-core")
    integrationTestImplementation(libs.testcontainers.postgresql)
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
    integrationTestImplementation(libs.wiremock.standalone)
}

// TransferEventsIntegrationTest runs LocalStack with the same init scripts as the local compose. The directory is a
// relative-path input, so the absolute path of the clone does not enter the build cache key.
abstract class LocalStackInitScripts : CommandLineArgumentProvider {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val directory: DirectoryProperty

    override fun asArguments() = listOf("-Dtransfers.localstack.init-dir=${directory.get().asFile.absolutePath}")
}

tasks.named<Test>("integrationTest") {
    jvmArgumentProviders.add(
        objects.newInstance<LocalStackInitScripts>().apply {
            directory.set(rootProject.layout.projectDirectory.dir("infra/local/localstack/init/ready.d"))
        },
    )
}
