plugins { id("digitalbanking.spring-boot-app") }

dependencies {
    implementation(project(":services:transfers:adapters"))
    implementation(project(":services:transfers:domain"))
    implementation(project(":services:transfers:application"))
    implementation(project(":libs:outbox"))
    implementation(project(":libs:outbox-relay"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    testImplementation(project(":libs:testing-architecture"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    integrationTestImplementation(testFixtures(project(":libs:outbox")))
    integrationTestImplementation(project(":libs:testing-localstack"))
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-jdbc")
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
