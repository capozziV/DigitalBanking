plugins {
    id("digitalbanking.spring-module")
    id("digitalbanking.contract-consumer")
}

dependencies {
    implementation(project(":services:statement:domain"))
    implementation(project(":services:statement:application"))

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

    // AccountEventsContractTest delivers the messages of the contracts of accounts to the listener.
    contractStubs(project(":services:accounts:bootstrap"))
}
