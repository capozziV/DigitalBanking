// Shared JWT resource server setup (Caller resolution, 401/403 as Problem Details), the machine token client and
// the client disconnect handling. Services depend on it from adapters; their application module depends on
// :libs:security:caller only.
plugins {
    id("digitalbanking.spring-module")
    id("digitalbanking.integration-test")
}

dependencies {
    api(project(":libs:security:caller"))
    api("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    api("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.slf4j:slf4j-api")
    implementation("tools.jackson.core:jackson-databind")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    integrationTestImplementation(project(":libs:testing-security"))
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-test")
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-actuator")
}
