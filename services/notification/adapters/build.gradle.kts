plugins { id("digitalbanking.spring-module") }

dependencies {
    implementation(project(":services:notification:domain"))
    implementation(project(":services:notification:application"))
    // Only for OutboxEventEnvelope.CORRELATION_ID_MDC_KEY, the MDC key shared by every service.
    implementation(project(":libs:outbox"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation(libs.java.uuid.generator)

    // The project's AWS SDK BOM sits next to the Spring Cloud AWS one so its (newer) SDK version wins.
    implementation(platform(libs.spring.cloud.aws.bom))
    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.spring.cloud.aws.starter.sqs)
    // Spring Cloud AWS only registers its SQS message converter when Jackson 3 (tools.jackson) is on the classpath.
    implementation("org.springframework.boot:spring-boot-starter-jackson")
    implementation("tools.jackson.module:jackson-module-kotlin")
}
