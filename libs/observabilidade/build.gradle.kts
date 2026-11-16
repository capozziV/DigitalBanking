// Correlation ID: the inbound filter (header, MDC, response header) and the RestClient interceptor that propagates it.
// Event propagation stays in :libs:outbox.
plugins {
    id("digitalbanking.spring-module")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.slf4j:slf4j-api")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Only to check that the MDC key is the one the outbox writer reads.
    testImplementation(project(":libs:outbox"))
}
