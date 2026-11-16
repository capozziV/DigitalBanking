plugins { id("digitalbanking.spring-module") }

dependencies {
    implementation(project(":services:statement:domain"))
    implementation(project(":services:statement:application"))
    // Only for OutboxEventEnvelope.CORRELATION_ID_MDC_KEY, the MDC key shared by every service.
    // statement has no relational database: the outbox's JDBC starter would make Spring Boot require a DataSource.
    implementation(project(":libs:outbox")) {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-jdbc")
    }

    implementation(project(":libs:security"))
    implementation(project(":libs:observabilidade"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // The project's AWS SDK BOM sits next to the Spring Cloud AWS one so its (newer) SDK version wins.
    implementation(platform(libs.spring.cloud.aws.bom))
    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.spring.cloud.aws.starter.sqs)
    implementation(libs.aws.sdk.dynamodb)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("io.micrometer:micrometer-core")
    // Spring Cloud AWS only registers its SQS message converter when Jackson 3 (tools.jackson) is on the classpath.
    implementation("org.springframework.boot:spring-boot-starter-jackson")
    implementation("tools.jackson.module:jackson-module-kotlin")
}
