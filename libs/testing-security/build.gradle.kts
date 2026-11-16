// Test support for services protected by :libs:security: an RSA key generated per JVM, a JwtDecoder that trusts it
// and signed tokens for customers, admins and services. Consume it only from integrationTestImplementation.
plugins {
    id("digitalbanking.spring-module")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    api("org.springframework:spring-test")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
