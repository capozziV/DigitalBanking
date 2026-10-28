plugins { id("digitalbanking.spring-boot-app") }

dependencies {
    implementation(project(":services:notification:adapters"))
    implementation(project(":services:notification:domain"))
    implementation(project(":services:notification:application"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // The project's AWS SDK BOM sits next to the Spring Cloud AWS one so its (newer) SDK version wins.
    implementation(platform(libs.spring.cloud.aws.bom))
    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.spring.cloud.aws.starter.sqs)

    testImplementation(project(":libs:testing-architecture"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    integrationTestImplementation(project(":libs:testing-localstack"))
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
}
