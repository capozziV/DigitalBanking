// Correlation ID: the inbound filter (header, MDC, response header) and the RestClient interceptor that propagates it.
// Event propagation stays in :libs:outbox.
plugins {
    id("digitalbanking.spring-module")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.slf4j:slf4j-api")
    // Only to document X-Correlation-Id in the services that publish an OpenAPI contract.
    compileOnly(libs.springdoc.openapi.starter.webmvc.ui)
    // The SQS interceptor is registered only when the service brings Spring Cloud AWS SQS itself.
    compileOnly(platform(libs.spring.cloud.aws.bom))
    compileOnly(libs.spring.cloud.aws.starter.sqs)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(platform(libs.spring.cloud.aws.bom))
    testImplementation(platform(libs.aws.sdk.bom))
    testImplementation(libs.spring.cloud.aws.starter.sqs)
    testImplementation(libs.springdoc.openapi.starter.webmvc.ui)
    // Only to check that the MDC key is the one the outbox writer reads.
    testImplementation(project(":libs:outbox"))
}
