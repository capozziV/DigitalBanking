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

// OutboxRelayIT runs LocalStack with the same init scripts as the local compose.
val localStackInitDir = rootProject.layout.projectDirectory.dir("infra/local/localstack/init/ready.d")
tasks.named<Test>("integrationTest") {
    inputs.dir(localStackInitDir).withPropertyName("localStackInitScripts")
    systemProperty("accounts.localstack.init-dir", localStackInitDir.asFile.absolutePath)
}
