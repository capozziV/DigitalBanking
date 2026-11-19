// Producer side of the message contracts (Stubborn Contract, ADR 0022): the generated contract tests receive the event
// the way the relay publishes it. The service writes it through its outbox mapper into a real outbox table
// (PostgreSQL in a container), and OutboxMessageVerifier reads the pending row back with the relay's own query and
// builds the envelope the relay sends to SNS. Consume it only from contractTestImplementation.
plugins {
    id("digitalbanking.spring-module")
    id("digitalbanking.integration-test")
}

dependencies {
    api(project(":libs:outbox"))
    api(project(":libs:outbox-relay"))
    api(platform(libs.stubborn.contract.bom))
    api("sh.stubborn:stubborn-contract-verifier")
    api("org.springframework.boot:spring-boot-starter-jdbc")
    implementation(libs.testcontainers.postgresql)
    implementation("tools.jackson.core:jackson-databind")
    runtimeOnly(libs.postgresql)

    integrationTestImplementation(platform(libs.junit.bom))
}
