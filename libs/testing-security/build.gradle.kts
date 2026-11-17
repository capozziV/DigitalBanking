// Test support for services protected by :libs:security: an RSA key generated per JVM, a JwtDecoder that trusts it,
// signed tokens for customers, admins and services, and the shared checks of the OpenAPI contract. Consume it only
// from integrationTestImplementation.
plugins {
    id("digitalbanking.spring-module")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    api("org.springframework:spring-test")
    // ApiDocsAssertions: the shared checks of every service's OpenAPI contract.
    implementation("org.assertj:assertj-core")
    // AwsProfileConfigAssertions: loads the configuration of a service with the aws profile.
    implementation("org.springframework.boot:spring-boot-test")
    implementation("tools.jackson.core:jackson-databind")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
