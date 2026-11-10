// Relay of the Transactional Outbox: reads the unpublished rows of a service's outbox table and publishes them.
// Services that only write to the outbox depend on :libs:outbox alone.
plugins {
    id("digitalbanking.spring-module")
    id("digitalbanking.integration-test")
}

dependencies {
    api(project(":libs:outbox"))
    api("io.micrometer:micrometer-core")
    api(platform(libs.aws.sdk.bom))
    api(libs.aws.sdk.sns)
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-starter")

    testImplementation("tools.jackson.core:jackson-databind")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    integrationTestImplementation(libs.testcontainers.postgresql)
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
    integrationTestRuntimeOnly(libs.postgresql)

    integrationTestImplementation(project(":libs:testing-localstack"))
}
