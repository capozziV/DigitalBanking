plugins { id("digitalbanking.spring-boot-app") }

// Explicit so bootJar resolves while the module has no sources yet; StatementApplication.kt (Kotlin `main`) arrives in task 4.
springBoot {
    mainClass = "io.github.capozziv.digitalbanking.statement.bootstrap.StatementApplicationKt"
}

dependencies {
    implementation(project(":services:statement:adapters"))
    implementation(project(":services:statement:domain"))
    implementation(project(":services:statement:application"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // The project's AWS SDK BOM sits next to the Spring Cloud AWS one so its (newer) SDK version wins.
    implementation(platform(libs.spring.cloud.aws.bom))
    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.spring.cloud.aws.starter.sqs)
    implementation(libs.aws.sdk.dynamodb)

    testImplementation(project(":libs:testing-architecture"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    integrationTestImplementation(project(":libs:testing-localstack"))
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
}
