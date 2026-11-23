plugins {
    id("digitalbanking.spring-module")
    id("digitalbanking.contract-consumer")
}

dependencies {
    implementation(project(":services:notification:domain"))
    implementation(project(":services:notification:application"))

    implementation(project(":libs:security"))
    implementation(project(":libs:observabilidade"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    // The migrations live here, next to the SQL that depends on them (same layout as accounts and transfers).
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)
    implementation(libs.java.uuid.generator)

    // The project's AWS SDK BOM sits next to the Spring Cloud AWS one so its (newer) SDK version wins.
    implementation(platform(libs.spring.cloud.aws.bom))
    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.spring.cloud.aws.starter.sqs)
    // Spring Cloud AWS only registers its SQS message converter when Jackson 3 (tools.jackson) is on the classpath.
    implementation("org.springframework.boot:spring-boot-starter-jackson")
    implementation("tools.jackson.module:jackson-module-kotlin")

    // EventContractsTest delivers the messages of the contracts of accounts, transfers and customer to the listener.
    contractStubs(project(":services:accounts:bootstrap"))
    contractStubs(project(":services:transfers:bootstrap"))
    contractStubs(project(":services:customer:bootstrap"))
}
