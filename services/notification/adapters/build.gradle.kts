plugins { id("digitalbanking.spring-module") }

dependencies {
    implementation(project(":services:notification:domain"))
    implementation(project(":services:notification:application"))

    // The project's AWS SDK BOM sits next to the Spring Cloud AWS one so its (newer) SDK version wins.
    implementation(platform(libs.spring.cloud.aws.bom))
    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.spring.cloud.aws.starter.sqs)
    // Spring Cloud AWS only registers its SQS message converter when Jackson 3 (tools.jackson) is on the classpath.
    implementation("org.springframework.boot:spring-boot-starter-jackson")
    implementation("tools.jackson.module:jackson-module-kotlin")
}
