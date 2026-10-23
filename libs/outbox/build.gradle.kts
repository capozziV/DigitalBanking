plugins {
    id("digitalbanking.spring-module")
    id("digitalbanking.integration-test")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-jdbc")
    api("io.micrometer:micrometer-core")
    api(platform(libs.aws.sdk.bom))
    api(libs.aws.sdk.sns)
    implementation(libs.java.uuid.generator)
    implementation("org.slf4j:slf4j-api")
    implementation("tools.jackson.core:jackson-databind")

    testImplementation("tools.jackson.core:jackson-databind")

    integrationTestImplementation(libs.testcontainers.postgresql)
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
    integrationTestRuntimeOnly(libs.postgresql)

    integrationTestImplementation(project(":libs:testing-localstack"))
}
