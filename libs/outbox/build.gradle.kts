plugins {
    id("digitalbanking.spring-module")
    id("digitalbanking.integration-test")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-jdbc")
    implementation(libs.java.uuid.generator)
    implementation("org.slf4j:slf4j-api")

    integrationTestImplementation(libs.testcontainers.postgresql)
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
    integrationTestRuntimeOnly(libs.postgresql)
}
