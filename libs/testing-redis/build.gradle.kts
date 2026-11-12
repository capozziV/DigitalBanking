// Test support shared by the integration tests that run against Redis (services).
// Consume it only from integrationTestImplementation.
plugins {
    id("digitalbanking.kotlin-library")
    id("digitalbanking.integration-test")
}

dependencies {
    api(platform(libs.spring.boot.bom))
    api(libs.testcontainers.localstack)

    integrationTestImplementation(platform(libs.junit.bom))
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
}
