// Test support shared by the integration tests that run against LocalStack (libs/outbox, services).
// Consume it only from integrationTestImplementation.
plugins {
    id("digitalbanking.kotlin-library")
    id("digitalbanking.integration-test")
}

dependencies {
    api(platform(libs.spring.boot.bom))
    api(platform(libs.aws.sdk.bom))
    api(libs.testcontainers.localstack)
    api(libs.aws.sdk.sns)
    api(libs.aws.sdk.sqs)
    api(libs.aws.sdk.dynamodb)
    api("org.slf4j:slf4j-api")
    implementation(platform(libs.junit.bom))
    implementation("org.junit.jupiter:junit-jupiter-api")

    integrationTestImplementation(libs.testcontainers.junit.jupiter)
}
