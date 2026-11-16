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
    // The admin endpoint of the relay (OutboxAdminController): services already run Spring MVC and springdoc.
    implementation("org.springframework:spring-web")
    compileOnly(libs.springdoc.openapi.starter.webmvc.ui)
    // Binds the Kotlin request DTO through its constructor, as in the services.
    runtimeOnly("tools.jackson.module:jackson-module-kotlin")

    testImplementation("tools.jackson.core:jackson-databind")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    integrationTestImplementation(libs.testcontainers.postgresql)
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
    integrationTestRuntimeOnly(libs.postgresql)

    integrationTestImplementation(project(":libs:testing-localstack"))

    // The admin endpoint behind the security of the services.
    integrationTestImplementation(project(":libs:security"))
    integrationTestImplementation(project(":libs:testing-security"))
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    integrationTestImplementation(libs.springdoc.openapi.starter.webmvc.ui)
}
