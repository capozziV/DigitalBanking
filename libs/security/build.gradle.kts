// Shared JWT resource server setup (Caller resolution, 401/403 as Problem Details), the machine token client and
// the client disconnect handling. Services depend on it from adapters; their application module depends on
// :libs:security-caller only.
plugins {
    id("digitalbanking.spring-module")
    id("digitalbanking.integration-test")
}

dependencies {
    api(project(":libs:security-caller"))
    api("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    api("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.slf4j:slf4j-api")
    implementation("tools.jackson.core:jackson-databind")
    // Only to document the common error responses in the services that publish an OpenAPI contract.
    compileOnly(libs.springdoc.openapi.starter.webmvc.ui)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.springdoc.openapi.starter.webmvc.ui)

    integrationTestImplementation(project(":libs:testing-security"))
    // Services use both libs: proves 401/403 keep the X-Correlation-Id (the correlation filter runs first).
    integrationTestImplementation(project(":libs:observabilidade"))
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-test")
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-actuator")
    integrationTestImplementation(libs.springdoc.openapi.starter.webmvc.ui)
}
