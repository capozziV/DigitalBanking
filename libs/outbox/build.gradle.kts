plugins {
    id("digitalbanking.spring-module")
    id("digitalbanking.integration-test")
    `java-test-fixtures`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-jdbc")
    implementation(libs.java.uuid.generator)
    implementation("org.slf4j:slf4j-api")
    implementation("tools.jackson.core:jackson-databind")

    // assertOutboxSchemaMatchesModel: services check their migrated outbox table against the model DDL.
    testFixturesImplementation(platform(libs.spring.boot.bom))
    testFixturesApi("org.springframework.boot:spring-boot-starter-jdbc")
    testFixturesApi(libs.assertj.core)

    integrationTestImplementation(testFixtures(project()))
    integrationTestImplementation(libs.testcontainers.postgresql)
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
    integrationTestRuntimeOnly(libs.postgresql)
}
