// Test support shared by the integration tests that run against LocalStack (libs/outbox, services).
// Consume it only from integrationTestImplementation.
plugins {
    id("digitalbanking.kotlin-library")
}

dependencies {
    api(platform(libs.spring.boot.bom))
    api(platform(libs.aws.sdk.bom))
    api(libs.testcontainers.localstack)
    api(libs.aws.sdk.sns)
    api(libs.aws.sdk.sqs)
    implementation(platform(libs.junit.bom))
    implementation("org.junit.jupiter:junit-jupiter-api")
}
